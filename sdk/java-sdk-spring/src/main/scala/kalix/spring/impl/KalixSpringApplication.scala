/*
 * Copyright 2021 Lightbend Inc.
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

import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType

import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.FutureConverters.CompletionStageOps
import scala.jdk.OptionConverters.RichOption
import scala.reflect.ClassTag

import akka.Done
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kalix.javasdk.Kalix
import kalix.javasdk.action.Action
import kalix.javasdk.action.ActionCreationContext
import kalix.javasdk.action.ActionProvider
import kalix.javasdk.action.ReflectiveActionProvider
import kalix.javasdk.annotations.ViewId
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
import kalix.javasdk.workflowentity.ReflectiveWorkflowEntityProvider
import kalix.javasdk.workflowentity.WorkflowEntity
import kalix.javasdk.workflowentity.WorkflowEntityContext
import kalix.javasdk.workflowentity.WorkflowEntityProvider
import kalix.spring.KalixClient
import kalix.spring.WebClientProvider
import kalix.spring.impl.KalixSpringApplication.ActionCreationContextFactoryBean
import kalix.spring.impl.KalixSpringApplication.EventSourcedEntityContextFactoryBean
import kalix.spring.impl.KalixSpringApplication.KalixClientFactoryBean
import kalix.spring.impl.KalixSpringApplication.KalixComponentProvider
import kalix.spring.impl.KalixSpringApplication.MainClassProvider
import kalix.spring.impl.KalixSpringApplication.ValueEntityContextFactoryBean
import kalix.spring.impl.KalixSpringApplication.ViewCreationContextFactoryBean
import kalix.spring.impl.KalixSpringApplication.WebClientProviderFactoryBean
import kalix.spring.impl.KalixSpringApplication.WorkflowContextFactoryBean
import kalix.spring.BuildInfo
import kalix.spring.ComponentClient
import kalix.spring.impl.KalixSpringApplication.ComponentClientFactoryBean
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanCreationException
import org.springframework.beans.factory.FactoryBean
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
    classOf[WorkflowEntity[_]] ::
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
  class MainClassProvider(cglibMain: Class[_]) extends ClassPathScanningCandidateComponentProvider {

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
  class KalixComponentProvider(cglibMain: Class[_]) extends ClassPathScanningCandidateComponentProvider {

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

  abstract class ThreadLocalFactoryBean[T: ClassTag] extends FactoryBean[T] {
    val threadLocal = new ThreadLocal[T]
    def set(value: T) = threadLocal.set(value)
    override def getObject: T = threadLocal.get()
    override def getObjectType: Class[_] = implicitly[ClassTag[T]].runtimeClass
  }

  object ActionCreationContextFactoryBean extends ThreadLocalFactoryBean[ActionCreationContext] {
    // ActionCreationContext is a singleton, so strictly speaking this could return 'true'
    // However, we still need the ThreadLocal hack to let Spring have access to it.
    // and we don't want to give it direct access to it, because the impl is private (and better keep it so).
    // because the testkit uses another ActionCreationContext impl. Therefore we want it to be defined at runtime.
    override def isSingleton: Boolean = false
  }

  object EventSourcedEntityContextFactoryBean extends ThreadLocalFactoryBean[EventSourcedEntityContext] {
    override def isSingleton: Boolean = false // never!!
  }

  object WorkflowContextFactoryBean extends ThreadLocalFactoryBean[WorkflowEntityContext] {
    override def isSingleton: Boolean = false // never!!
  }

  object ValueEntityContextFactoryBean extends ThreadLocalFactoryBean[ValueEntityContext] {
    override def isSingleton: Boolean = false // never!!
  }

  object ViewCreationContextFactoryBean extends ThreadLocalFactoryBean[ViewCreationContext] {
    override def isSingleton: Boolean = false // never!!
  }

  object KalixClientFactoryBean extends ThreadLocalFactoryBean[KalixClient] {
    override def isSingleton: Boolean = true // yes, we only need one

    override def getObject: KalixClient =
      if (threadLocal.get() != null) threadLocal.get()
      else
        throw new BeanCreationException("KalixClient can only be injected in Kalix Actions and Workflows.")
  }

  object ComponentClientFactoryBean extends ThreadLocalFactoryBean[ComponentClient] {
    override def isSingleton: Boolean = true // yes, we only need one

    override def getObject: ComponentClient =
      if (threadLocal.get() != null) threadLocal.get()
      else
        throw new BeanCreationException("ComponentClient can only be injected in Kalix Actions and Workflows.")
  }

  object WebClientProviderFactoryBean extends ThreadLocalFactoryBean[WebClientProvider] {
    override def isSingleton: Boolean = true // yes, we only need one

    override def getObject: WebClientProvider =
      if (threadLocal.get() != null) threadLocal.get()
      else
        throw new BeanCreationException("WebClientProvider can only be injected in Kalix Actions and Workflows.")
  }
}

case class KalixSpringApplication(applicationContext: ApplicationContext, config: Config) {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private val messageCodec = new JsonMessageCodec
  private val kalixClient = new RestKalixClientImpl(messageCodec)
  private val componentClient = new ComponentClient(kalixClient)

  private val kalixBeanFactory = new DefaultListableBeanFactory(applicationContext)

  kalixBeanFactory.registerSingleton("actionCreationContextFactoryBean", ActionCreationContextFactoryBean)
  kalixBeanFactory.registerSingleton("eventSourcedEntityContext", EventSourcedEntityContextFactoryBean)
  kalixBeanFactory.registerSingleton("workflowEntityContext", WorkflowContextFactoryBean)
  kalixBeanFactory.registerSingleton("valueEntityContext", ValueEntityContextFactoryBean)
  kalixBeanFactory.registerSingleton("viewCreationContext", ViewCreationContextFactoryBean)
  kalixBeanFactory.registerSingleton("kalixClient", KalixClientFactoryBean)
  kalixBeanFactory.registerSingleton("componentClient", ComponentClientFactoryBean)
  kalixBeanFactory.registerSingleton("webClientProvider", WebClientProviderFactoryBean)

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
  val classBeanMap =
    provider.findKalixComponents.map { bean =>
      // here we need to load the components using the same loader as the Main class
      // this is needed to have it loaded in the RestartClassLoader when using auto-reload
      // see MainClassProvider.findOriginalMainClass where we load Main using same CL as cglibEnhanceMainClass
      mainClass.getClassLoader.loadClass(bean.getBeanClassName) -> bean
    }.toMap

  // each loaded class needs to be validated before registration
  val validation =
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

      if (classOf[WorkflowEntity[_]].isAssignableFrom(clz)) {
        logger.info(s"Registering Workflow provider for [${clz.getName}]")
        val workflow = workflowProvider(clz.asInstanceOf[Class[WorkflowEntity[Nothing]]])
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

  /* Each component may have a creation context passed to its constructor.
   * This method checks if there is a constructor in `clz` that receives a `context`.
   */
  private def hasContextConstructor(clz: Class[_], contextType: Class[_]): Boolean =
    clz.getConstructors.exists { ctor =>
      ctor.getParameterTypes.contains(contextType)
    }

  private def actionProvider[A <: Action](clz: Class[A]): ActionProvider[A] =
    ReflectiveActionProvider.of(
      clz,
      messageCodec,
      context => {
        if (hasContextConstructor(clz, classOf[ActionCreationContext]))
          ActionCreationContextFactoryBean.set(context)

        val webClientProviderHolder = WebClientProviderHolder(context.materializer().system)

        setKalixClient(clz, webClientProviderHolder)
        setComponentClient(clz, webClientProviderHolder)

        if (hasContextConstructor(clz, classOf[WebClientProvider])) {
          val webClientProvider = webClientProviderHolder.webClientProvider
          WebClientProviderFactoryBean.set(webClientProvider)
        }

        kalixBeanFactory.getBean(clz)
      })

  def getComponentClient(): ComponentClient = componentClient

  def getKalixClient(): RestKalixClientImpl = kalixClient

  private def setKalixClient[T](clz: Class[T], webClientProviderHolder: WebClientProviderHolder): Unit = {
    if (hasContextConstructor(clz, classOf[KalixClient])) {
      kalixClient.setWebClient(webClientProviderHolder.webClientProvider.localWebClient)
      // we only have one KalixClient, but we only set it to the ThreadLocalFactoryBean
      // when building actions, because it's only allowed to inject it in Actions and Workflow Entities
      KalixClientFactoryBean.set(kalixClient)
    }
  }

  private def setComponentClient[T](clz: Class[T], webClientProviderHolder: WebClientProviderHolder): Unit = {
    if (hasContextConstructor(clz, classOf[ComponentClient])) {
      kalixClient.setWebClient(webClientProviderHolder.webClientProvider.localWebClient)
      ComponentClientFactoryBean.set(componentClient)
    }
  }

  private def eventSourcedEntityProvider[S, E, ES <: EventSourcedEntity[S, E]](
      clz: Class[ES]): EventSourcedEntityProvider[S, E, ES] =
    ReflectiveEventSourcedEntityProvider.of(
      clz,
      messageCodec,
      context => {
        if (hasContextConstructor(clz, classOf[EventSourcedEntityContext]))
          EventSourcedEntityContextFactoryBean.set(context)
        kalixBeanFactory.getBean(clz)
      })

  private def workflowProvider[S, E <: WorkflowEntity[S]](clz: Class[E]): WorkflowEntityProvider[S, E] = {
    ReflectiveWorkflowEntityProvider.of(
      clz,
      messageCodec,
      context => {
        if (hasContextConstructor(clz, classOf[WorkflowEntityContext])) {
          WorkflowContextFactoryBean.set(context)
        }

        val webClientProviderHolder = WebClientProviderHolder(context.materializer().system)

        setKalixClient(clz, webClientProviderHolder)
        setComponentClient(clz, webClientProviderHolder)

        val workflowEntity = kalixBeanFactory.getBean(clz)

        val workflowStateType: Class[S] =
          workflowEntity.getClass.getGenericSuperclass
            .asInstanceOf[ParameterizedType]
            .getActualTypeArguments
            .head
            .asInstanceOf[Class[S]]

        messageCodec.lookupTypeHint(workflowStateType)

        workflowEntity
          .definition()
          .forEachStep {
            case asyncCallStep: WorkflowEntity.AsyncCallStep[_, _, _] =>
              messageCodec.lookupTypeHint(asyncCallStep.callInputClass)
              messageCodec.lookupTypeHint(asyncCallStep.transitionInputClass)
            case callStep: WorkflowEntity.CallStep[_, _, _, _] =>
              messageCodec.lookupTypeHint(callStep.callInputClass)
              messageCodec.lookupTypeHint(callStep.transitionInputClass)
          }

        workflowEntity
      })
  }

  private def valueEntityProvider[S, E <: ValueEntity[S]](clz: Class[E]): ValueEntityProvider[S, E] =
    ReflectiveValueEntityProvider.of(
      clz,
      messageCodec,
      context => {
        if (hasContextConstructor(clz, classOf[ValueEntityContext]))
          ValueEntityContextFactoryBean.set(context)
        kalixBeanFactory.getBean(clz)
      })

  private def viewProvider[S, V <: View[S]](clz: Class[V]): ViewProvider =
    ReflectiveViewProvider.of[S, V](
      clz,
      messageCodec,
      context => {
        if (hasContextConstructor(clz, classOf[ViewCreationContext]))
          ViewCreationContextFactoryBean.set(context)
        kalixBeanFactory.getBean(clz)
      })

  private def multiTableViewProvider[V](clz: Class[V]): ViewProvider =
    ReflectiveMultiTableViewProvider.of[V](
      clz,
      messageCodec,
      (viewTableClass, context) => {
        if (hasContextConstructor(viewTableClass, classOf[ViewCreationContext]))
          ViewCreationContextFactoryBean.set(context)
        kalixBeanFactory.getBean(viewTableClass)
      })
}
