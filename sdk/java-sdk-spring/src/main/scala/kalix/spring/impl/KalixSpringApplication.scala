/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kalix.spring.impl

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.FutureConverters.CompletionStageOps
import scala.jdk.OptionConverters.RichOption
import akka.Done
import com.typesafe.config.Config
import io.opentelemetry.api.trace.Tracer
import kalix.javasdk.Context
import kalix.javasdk.Kalix
import kalix.javasdk.action.Action
import kalix.javasdk.action.ActionCreationContext
import kalix.javasdk.action.ActionProvider
import kalix.javasdk.action.ReflectiveActionProvider
import kalix.javasdk.annotations.ViewId
import kalix.javasdk.client.ComponentClient
import kalix.javasdk.eventsourced.ReflectiveEventSourcedEntityProvider
import kalix.javasdk.eventsourcedentity.EventSourcedEntity
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext
import kalix.javasdk.eventsourcedentity.EventSourcedEntityProvider
import kalix.javasdk.impl.AclDescriptorFactory
import kalix.javasdk.impl.JsonMessageCodec
import kalix.javasdk.impl.Validations
import kalix.javasdk.impl.Validations.Invalid
import kalix.javasdk.impl.Validations.Valid
import kalix.javasdk.impl.Validations.Validation
import kalix.javasdk.replicatedentity.ReplicatedEntity
import kalix.javasdk.valueentity.ReflectiveValueEntityProvider
import kalix.javasdk.valueentity.ValueEntity
import kalix.javasdk.valueentity.ValueEntityContext
import kalix.javasdk.valueentity.ValueEntityProvider
import kalix.javasdk.view.ReflectiveMultiTableViewProvider
import kalix.javasdk.view.ReflectiveViewProvider
import kalix.javasdk.view.View
import kalix.javasdk.view.ViewCreationContext
import kalix.javasdk.view.ViewProvider
import kalix.javasdk.workflow.AbstractWorkflow
import kalix.javasdk.workflow.ReflectiveWorkflowProvider
import kalix.javasdk.workflow.Workflow
import kalix.javasdk.workflow.WorkflowContext
import kalix.javasdk.workflow.WorkflowProvider
import kalix.spring.BuildInfo
import kalix.spring.KalixClient
import kalix.spring.WebClientProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.`type`.classreading.MetadataReader
import org.springframework.core.`type`.classreading.MetadataReaderFactory
import org.springframework.core.`type`.filter.TypeFilter

object KalixSpringApplication {

  val kalixComponents: Seq[Class[_]] =
    classOf[Action] ::
    classOf[EventSourcedEntity[_, _]] ::
    classOf[Workflow[_]] ::
    classOf[ValueEntity[_]] ::
    classOf[ReplicatedEntity[_]] ::
    classOf[View[_]] ::
    Nil

  private val kalixComponentsNames = kalixComponents.map(_.getName)

  /**
   * A multi-table view has a ViewId annotation, doesn't extend View itself, but contains at least one View class.
   */
  def isMultiTableView(component: Class[_]): Boolean = {
    (component.getAnnotation(classOf[ViewId]) ne null) &&
    !classOf[View[_]].isAssignableFrom(component) &&
    component.getDeclaredClasses.exists(classOf[View[_]].isAssignableFrom)
  }

  /**
   * A view table component is a View which is a nested class (static member class) of a multi-table view.
   */
  def isNestedViewTable(component: Class[_]): Boolean = {
    classOf[View[_]].isAssignableFrom(component) &&
    (component.getDeclaringClass ne null) &&
    Modifier.isStatic(component.getModifiers) &&
    (component.getDeclaringClass.getAnnotation(classOf[ViewId]) ne null)
  }

  /**
   * Classpath scanning provider that will lookup for the original main class. Spring doesn't make the original main
   * class available in the application context, but a cglib enhanced variant.
   *
   * The enhanced variant doesn't contain all the annotations, but only the SpringBootApplication one. Therefore, we
   * need to lookup for the original one. We need it to find the default ACL annotation.
   */
  private class MainClassProvider(cglibMain: Class[_]) extends ClassPathScanningCandidateComponentProvider {

    private object OriginalMainClassFilter extends TypeFilter {
      override def `match`(metadataReader: MetadataReader, metadataReaderFactory: MetadataReaderFactory): Boolean = {
        // in the classpath, we should have another class annotated with SpringBootApplication
        // this is the original class that generated the cglib enhanced one
        metadataReader.getAnnotationMetadata.hasAnnotation(classOf[SpringBootApplication].getName)
      }
    }

    addIncludeFilter(OriginalMainClassFilter)

    def findOriginalMainClass: Class[_] =
      this
        .findCandidateComponents(cglibMain.getPackageName)
        .asScala
        .map { bean =>
          // to avoid surprises, we load it using same classloader as the cglibMain
          cglibMain.getClassLoader.loadClass(bean.getBeanClassName)
        }
        .head

  }

  /**
   * Kalix components are not Spring components. They should not be wired into other components and they should not be
   * freely available for users to access.
   *
   * Therefore, we should block the usage of any Spring stereotype annotations. As a consequence, they won't be
   * available in the app's ApplicationContext and we need to scan the classpath ourselves in order to register them.
   *
   * This class will do exactly this. It find them and return tweaked BeanDefinitions (eg :prototype scope and autowired
   * by constructor)
   */
  private class KalixComponentProvider(cglibMain: Class[_]) extends ClassPathScanningCandidateComponentProvider {

    private object KalixComponentTypeFilter extends TypeFilter {
      override def `match`(metadataReader: MetadataReader, metadataReaderFactory: MetadataReaderFactory): Boolean = {
        kalixComponentsNames.contains(metadataReader.getClassMetadata.getSuperClassName) ||
        multiTableViewMatch(metadataReader)
      }

      /**
       * A potential multi-table view component has a ViewId annotation but doesn't extend View.
       */
      private def multiTableViewMatch(metadataReader: MetadataReader): Boolean = {
        metadataReader.getAnnotationMetadata.hasAnnotation(classOf[ViewId].getName) &&
        metadataReader.getClassMetadata.getSuperClassName != classOf[View[_]].getName
      }
    }

    addIncludeFilter(KalixComponentTypeFilter)

    // TODO: users may define their Kalix components in other packages as well and then use @ComponentScan
    // to let Spring find them. We should also look for @ComponentScan in the Main class and collect any
    // scan package declared there. So later, packageToScan will be a List of packages
    def findKalixComponents: Seq[BeanDefinition] = {
      findCandidateComponents(cglibMain.getPackageName).asScala.map { bean =>
        // by default, the provider set them all as singletons,
        // we need to make them all a prototype
        bean.setScope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)

        // making it only wireable by constructor will simplify our lives
        // we can review it later, if needed
        bean.asInstanceOf[AbstractBeanDefinition].setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR)
        bean

      }.toSeq
    }
  }

}

case class KalixSpringApplication(applicationContext: ApplicationContext, config: Config) {

  import KalixSpringApplication._

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val messageCodec = new JsonMessageCodec
  private[kalix] val kalixClient = new RestKalixClientImpl(messageCodec)
  private[kalix] val componentClient = new ComponentClient(kalixClient)

  private val kalixBeanFactory = new DefaultListableBeanFactory(applicationContext)

  // there should be only one class annotated with SpringBootApplication in the applicationContext
  private val cglibEnhanceMainClass =
    applicationContext.getBeansWithAnnotation(classOf[SpringBootApplication]).values().asScala.head

  // lookup for the original main class, not the one enhanced by CGLIB
  private val mainClass = new MainClassProvider(cglibEnhanceMainClass.getClass).findOriginalMainClass

  val kalix: Kalix = (new Kalix)
    .withSdkName(BuildInfo.name)
    .withDefaultAclFileDescriptor(AclDescriptorFactory.defaultAclFileDescriptor(mainClass).toJava)

  private val provider = new KalixComponentProvider(cglibEnhanceMainClass.getClass)
  provider.setEnvironment(applicationContext.getEnvironment) //use the same environment to get access to properties

  // load all Kalix components found in the classpath
  private val classBeanMap =
    provider.findKalixComponents.map { bean =>
      // here we need to load the components using the same loader as the Main class
      // this is needed to have it loaded in the RestartClassLoader when using auto-reload
      // see MainClassProvider.findOriginalMainClass where we load Main using same CL as cglibEnhanceMainClass
      mainClass.getClassLoader.loadClass(bean.getBeanClassName) -> bean
    }.toMap

  // each loaded class needs to be validated before registration
  private val validation =
    classBeanMap.keySet
      .foldLeft(Valid: Validation) { case (validations, cls) =>
        validations ++ Validations.validate(cls)
      }

  validation match { // if any invalid component, log and throw
    case Valid => ()
    case Invalid(messages) =>
      messages.foreach { msg => logger.error(msg) }
      validation.failIfInvalid
  }

  // register them if all valid
  classBeanMap
    .foreach { case (clz, bean) =>
      kalixBeanFactory.registerBeanDefinition(bean.getBeanClassName, bean)

      if (classOf[Action].isAssignableFrom(clz)) {
        logger.info(s"Registering Action provider for [${clz.getName}]")
        val action = actionProvider(clz.asInstanceOf[Class[Action]])
        kalix.register(action)
        kalixClient.registerComponent(action.serviceDescriptor())
      }

      if (classOf[EventSourcedEntity[_, _]].isAssignableFrom(clz)) {
        logger.info(s"Registering EventSourcedEntity provider for [${clz.getName}]")
        val esEntity = eventSourcedEntityProvider(clz.asInstanceOf[Class[EventSourcedEntity[Nothing, Nothing]]])
        kalix.register(esEntity)
        kalixClient.registerComponent(esEntity.serviceDescriptor())
      }

      if (classOf[Workflow[_]].isAssignableFrom(clz)) {
        logger.info(s"Registering Workflow provider for [${clz.getName}]")
        val workflow = workflowProvider(clz.asInstanceOf[Class[Workflow[Nothing]]])
        kalix.register(workflow)
        kalixClient.registerComponent(workflow.serviceDescriptor())
      }

      if (classOf[ValueEntity[_]].isAssignableFrom(clz)) {
        logger.info(s"Registering ValueEntity provider for [${clz.getName}]")
        val valueEntity = valueEntityProvider(clz.asInstanceOf[Class[ValueEntity[Nothing]]])
        kalix.register(valueEntity)
        kalixClient.registerComponent(valueEntity.serviceDescriptor())
      }

      if (classOf[View[_]].isAssignableFrom(clz) && !KalixSpringApplication.isNestedViewTable(clz)) {
        logger.info(s"Registering View provider for [${clz.getName}]")
        val view = viewProvider(clz.asInstanceOf[Class[View[Nothing]]])
        kalix.register(view)
        kalixClient.registerComponent(view.serviceDescriptor())
      }

      if (KalixSpringApplication.isMultiTableView(clz)) {
        logger.info(s"Registering multi-table View provider for [${clz.getName}]")
        val view = multiTableViewProvider(clz)
        kalix.register(view)
        kalixClient.registerComponent(view.serviceDescriptor())
      }
    }

  private lazy val kalixRunner = kalix.createRunner(config)

  def start(): Future[Done] = {
    logger.info("Starting Kalix Application...")
    kalixRunner.run().asScala
  }

  def stop(): Future[Done] = {
    logger.info("Stopping Kalix Application...")
    kalixRunner.terminate().asScala
  }

  def port: Int = kalixRunner.configuration.userFunctionPort

  private def webClientProvider(context: Context) = {
    val webClientProviderHolder = WebClientProviderHolder(context.materializer().system)
    webClientProviderHolder.webClientProvider
  }

  private def kalixClient(context: Context): KalixClient = {
    kalixClient.setWebClient(webClientProvider(context).localWebClient)
    kalixClient
  }

  private def componentClient(context: Context): ComponentClient = {
    kalixClient.setWebClient(webClientProvider(context).localWebClient)
    componentClient
  }

  /**
   * Create an instance of `clz` using the mappings defined in `partial`. Each component provider should define what are
   * the acceptable dependencies in the partial function.
   *
   * If the partial function doesn't match, it will try to lookup in the Spring applicationContext.
   */
  private def wiredInstance[T](clz: Class[T])(partial: PartialFunction[Class[_], Any]): T = {
    // only one constructor allowed
    require(clz.getDeclaredConstructors.length == 1, s"Class [${clz.getSimpleName}] must have only one constructor.")
    wiredInstance(clz.getDeclaredConstructors.head.asInstanceOf[Constructor[T]])(partial)
  }

  /**
   * Create an instance using the passed `constructor` and the mappings defined in `partial`.
   *
   * Each component provider should define what are the acceptable dependencies in the partial function.
   *
   * If the partial function doesn't match, it will try to lookup in the Spring applicationContext.
   */
  private def wiredInstance[T](constructor: Constructor[T])(partial: PartialFunction[Class[_], Any]): T = {

    // Note that this function is total because it will always return a value (even if null)
    // last case is a catch all that lookups in the applicationContext
    val totalWireFunction: PartialFunction[Class[_], Any] =
      partial.orElse {
        case p if p == classOf[Config] => kalixRunner.finalConfig
        // block wiring of clients into anything that is not an Action or Workflow
        // NOTE: if they are allowed, 'partial' should already have a matching case for them
        case p if p == classOf[KalixClient] =>
          throw new BeanCreationException(
            s"[${constructor.getDeclaringClass.getSimpleName}] are not allowed to have a dependency on KalixClient")

        case p if p == classOf[ComponentClient] =>
          throw new BeanCreationException(
            s"[${constructor.getDeclaringClass.getSimpleName}] are not allowed to have a dependency on ComponentClient")

        case p if p == classOf[WebClientProvider] =>
          throw new BeanCreationException(
            s"[${constructor.getDeclaringClass.getSimpleName}] are not allowed to have a dependency on WebClientProvider")

        // if partial func doesn't match, try to lookup in the applicationContext
        case anyOther =>
          val bean = applicationContext.getBean(anyOther)
          if (bean == null)
            throw new BeanCreationException(
              s"Cannot wire [${anyOther.getSimpleName}]. Bean not found in the Application Context");
          else bean
      }

    // all params must be wired so we use 'map' not 'collect'
    val params = constructor.getParameterTypes.map(totalWireFunction)

    constructor.newInstance(params: _*)
  }

  private def actionProvider[A <: Action](clz: Class[A]): ActionProvider[A] =
    ReflectiveActionProvider.of(
      clz,
      messageCodec,
      context =>
        wiredInstance(clz) {
          case p if p == classOf[ActionCreationContext] => context
          case p if p == classOf[KalixClient]           => kalixClient(context)
          case p if p == classOf[ComponentClient]       => componentClient(context)
          case p if p == classOf[WebClientProvider]     => webClientProvider(context)
          case p if p == classOf[Tracer]                => context.getTracer
        })

  private def workflowProvider[S, W <: Workflow[S]](clz: Class[W]): WorkflowProvider[S, W] = {
    ReflectiveWorkflowProvider.of(
      clz,
      messageCodec,
      context => {

        val workflow =
          wiredInstance(clz) {
            case p if p == classOf[WorkflowContext]   => context
            case p if p == classOf[KalixClient]       => kalixClient(context)
            case p if p == classOf[ComponentClient]   => componentClient(context)
            case p if p == classOf[WebClientProvider] => webClientProvider(context)
          }

        val workflowStateType: Class[S] =
          workflow.getClass.getGenericSuperclass
            .asInstanceOf[ParameterizedType]
            .getActualTypeArguments
            .head
            .asInstanceOf[Class[S]]

        messageCodec.registerTypeHints(workflowStateType)

        workflow
          .definition()
          .getSteps
          .asScala
          .flatMap {
            case asyncCallStep: AbstractWorkflow.AsyncCallStep[_, _, _] =>
              List(asyncCallStep.callInputClass, asyncCallStep.transitionInputClass)
            case callStep: AbstractWorkflow.CallStep[_, _, _, _] =>
              List(callStep.callInputClass, callStep.transitionInputClass)
          }
          .foreach(messageCodec.registerTypeHints)

        workflow
      })
  }

  private def eventSourcedEntityProvider[S, E, ES <: EventSourcedEntity[S, E]](
      clz: Class[ES]): EventSourcedEntityProvider[S, E, ES] =
    ReflectiveEventSourcedEntityProvider.of(
      clz,
      messageCodec,
      context =>
        wiredInstance(clz) {
          case p if p == classOf[EventSourcedEntityContext] => context
        })

  private def valueEntityProvider[S, VE <: ValueEntity[S]](clz: Class[VE]): ValueEntityProvider[S, VE] =
    ReflectiveValueEntityProvider.of(
      clz,
      messageCodec,
      context =>
        wiredInstance(clz) {
          case p if p == classOf[ValueEntityContext] => context
        })

  private def viewProvider[S, V <: View[S]](clz: Class[V]): ViewProvider =
    ReflectiveViewProvider.of[S, V](
      clz,
      messageCodec,
      context =>
        wiredInstance(clz) {
          case p if p == classOf[ViewCreationContext] => context
        })

  private def multiTableViewProvider[V](clz: Class[V]): ViewProvider =
    ReflectiveMultiTableViewProvider.of[V](
      clz,
      messageCodec,
      (viewTableClass, context) => {
        val constructor = viewTableClass.getConstructors.head.asInstanceOf[Constructor[View[_]]]
        wiredInstance(constructor) {
          case p if p == classOf[ViewCreationContext] => context
        }
      })
}
