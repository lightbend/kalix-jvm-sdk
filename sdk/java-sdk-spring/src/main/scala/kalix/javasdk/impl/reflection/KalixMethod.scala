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

package kalix.javasdk.impl.reflection

import java.lang.reflect.Method
import scala.annotation.tailrec
import com.google.protobuf.Descriptors
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.impl.AclDescriptorFactory
import kalix.javasdk.impl.path.PathPattern
import kalix.javasdk.impl.path.PathPatternParser
import kalix.javasdk.impl.reflection.RestServiceIntrospector.PathParameter
import kalix.javasdk.impl.reflection.RestServiceIntrospector.RestMethodParameter
import kalix.javasdk.impl.reflection.RestServiceIntrospector.isEmpty
import kalix.javasdk.impl.reflection.RestServiceIntrospector.validateRequestMapping
import org.springframework.web.bind.annotation.PathVariable
// TODO: abstract away spring dependency
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
// TODO: abstract away reactor dependency
import reactor.core.publisher.Flux

object ServiceMethod {
  def isStreamOut(method: Method): Boolean =
    method.getReturnType == classOf[Flux[_]]

  // this is more for early validation. We don't support stream-in over http,
  // we block it before deploying anything
  def isStreamIn(method: Method): Boolean = {
    // TODO: remove this once is validations completely moved to Validations object
    val paramWithRequestBody =
      method.getParameters.collect {
        case param if param.getAnnotation(classOf[RequestBody]) != null => param
      }

    if (paramWithRequestBody.exists(_.getType == classOf[Flux[_]]))
      throw new IllegalArgumentException("Stream in calls are not supported")
    else
      false
  }
}
sealed trait ServiceMethod {
  def methodName: String
  def javaMethodOpt: Option[Method]

  def streamIn: Boolean
  def streamOut: Boolean
}

sealed trait AnyJsonRequestServiceMethod extends ServiceMethod {
  def inputType: Class[_]
}

/**
 * Build from methods annotated with @Subscription at type level.
 *
 * It's used as a 'virtual' method because there is no Java method backing it. It will exist only in the gRPC descriptor
 * and will be used for view updates with transform = false
 */
case class VirtualServiceMethod(component: Class[_], methodName: String, inputType: Class[_])
    extends AnyJsonRequestServiceMethod {

  override def javaMethodOpt: Option[Method] = None

  val streamIn: Boolean = false
  val streamOut: Boolean = false
}

case class CombinedSubscriptionServiceMethod(
    componentName: String,
    combinedMethodName: String,
    methodsMap: Map[String, Method])
    extends AnyJsonRequestServiceMethod {

  val methodName: String = combinedMethodName
  override def inputType: Class[_] = classOf[ScalaPbAny]

  override def javaMethodOpt: Option[Method] = None

  val streamIn: Boolean = false
  val streamOut: Boolean = false
}

/**
 * Build from methods annotated with @Subscription. Those methods are not annotated with Spring REST annotations and are
 * only used internally (between proxy and user function).
 */
case class SubscriptionServiceMethod(javaMethod: Method) extends AnyJsonRequestServiceMethod {

  val methodName: String = javaMethod.getName
  val inputType: Class[_] = javaMethod.getParameterTypes()(0)

  override def javaMethodOpt: Option[Method] = Some(javaMethod)

  val streamIn: Boolean = ServiceMethod.isStreamIn(javaMethod)
  val streamOut: Boolean = ServiceMethod.isStreamOut(javaMethod)
}

/**
 * Additional trait to simplify pattern matching for actual and virtual delete service method
 */
trait DeleteServiceMethod extends ServiceMethod

/**
 * A special case for subscription method with arity zero, in comparison to SubscriptionServiceMethod with required
 * arity one.
 */
case class HandleDeletesServiceMethod(javaMethod: Method) extends DeleteServiceMethod {
  override def methodName: String = javaMethod.getName

  override def javaMethodOpt: Option[Method] = Some(javaMethod)

  override def streamIn: Boolean = false

  override def streamOut: Boolean = false
}

/**
 * Similar to VirtualServiceMethod but for deletes.
 */
case class VirtualDeleteServiceMethod(component: Class[_], methodName: String) extends DeleteServiceMethod {

  override def javaMethodOpt: Option[Method] = None

  override def streamIn: Boolean = false

  override def streamOut: Boolean = false
}

/**
 * Build from Spring annotations
 *
 * @param callable
 *   Is this actually a method that will ever be called or just there for metadata annotations?
 */
case class SyntheticRequestServiceMethod(
    classMapping: Option[RequestMapping],
    mapping: RequestMapping,
    javaMethod: Method,
    params: Seq[RestMethodParameter],
    callable: Boolean = true)
    extends ServiceMethod {

  override def javaMethodOpt: Option[Method] = Some(javaMethod)
  override def methodName: String = javaMethod.getName

  val streamIn: Boolean = ServiceMethod.isStreamIn(javaMethod)
  val streamOut: Boolean = ServiceMethod.isStreamOut(javaMethod)

  // First fail on unsupported mapping values. Should all default to empty arrays, but let's not trust that
  validateRequestMapping(javaMethod, mapping)
  if (!isEmpty(mapping.method()) && classMapping.exists(cm => !isEmpty(cm.method()))) {
    throw ServiceIntrospectionException(
      javaMethod,
      "Invalid request method mapping. A request method mapping may only be defined on the class, or on the method, but not both.")
  }
  if (isEmpty(mapping.path()) && classMapping.forall(cm => isEmpty(cm.path()))) {
    throw ServiceIntrospectionException(
      javaMethod,
      "Missing path mapping. Kalix Java SDK methods must have a path defined.")
  }
  if (isEmpty(mapping.method()) && classMapping.forall(cm => isEmpty(cm.method()))) {
    throw ServiceIntrospectionException(
      javaMethod,
      "Missing request method mapping. Kalix Java SDK methods must have a request method defined.")
  }

  if (javaMethod.getParameterAnnotations.toSet.flatten
      .collect { case (p: PathVariable) => p }
      .exists(_.required() == false)) {

    throw ServiceIntrospectionException(javaMethod, "Currently all @PathVariables must be defined as required.")
  }

  private val pathFromAnnotation: String = {
    val classPath = classMapping match {
      case Some(cm) if !isEmpty(cm.path) =>
        cm.path().head
      case _ => ""
    }
    classPath + (mapping.path match {
      case Array(path) => path
      case _           => ""
    })
  }

  val parsedPath: PathPattern = PathPatternParser.parse(pathFromAnnotation)

  val pathTemplate: String = parsedPath.toGrpcTranscodingPattern

  val pathParameters: Seq[PathParameter] = params.collect { case p: PathParameter => p }

  // Validate all the path parameters exist
  pathParameters.foreach { param =>
    if (!parsedPath.fields.contains(param.name)) {
      throw ServiceIntrospectionException(
        param.param.getAnnotatedElement,
        s"There is no parameter named ${param.name} in the path pattern for this method.")
    }
  }

  val requestMethod: RequestMethod = {
    mapping.method match {
      case Array(method) => method
      case _             =>
        // This has already been validated so can't fail
        classMapping.get.method.head
    }
  }
}

object KalixMethod {
  def apply(
      serviceMethod: ServiceMethod,
      methodOptions: Option[kalix.MethodOptions] = None,
      entityIds: Seq[String] = Seq.empty): KalixMethod = {

    val aclOptions =
      serviceMethod.javaMethodOpt.flatMap { meth =>
        AclDescriptorFactory.methodLevelAclAnnotation(meth)
      }

    new KalixMethod(serviceMethod, methodOptions, entityIds)
      .withKalixOptions(aclOptions)
  }
}

case class KalixMethod private (
    serviceMethod: ServiceMethod,
    methodOptions: Option[kalix.MethodOptions] = None,
    entityIds: Seq[String] = Seq.empty) {

  /**
   * KalixMethod is used to collect all the information that we need to produce a gRPC method for the proxy. At the end
   * of the road, we need to check if any incompatibility was created. Therefore the validation should occur when we
   * finish to scan the component and are ready to build the gRPC method.
   *
   * For example, a method eventing.in method with an ACL annotation.
   */
  def validate(): Unit = {
    // check if eventing.in and acl are mixed
    methodOptions.foreach { opts =>
      if (opts.getEventing.hasIn && opts.hasAcl)
        throw ServiceIntrospectionException(
          // safe call: ServiceMethods without a java counterpart won't have ACL anyway
          serviceMethod.javaMethodOpt.get,
          "Subscription methods are for internal use only and cannot be combined with ACL annotations.")
    }
  }

  /**
   * This method merges the new method options with the existing ones. In case of collision the 'opts' are kept
   *
   * @param opts
   * @return
   */
  def withKalixOptions(opts: kalix.MethodOptions): KalixMethod =
    copy(methodOptions = Some(mergeKalixOptions(methodOptions, opts)))

  /**
   * This method merges the new method options with the existing ones. In case of collision the 'opts' are kept
   * @param opts
   * @return
   */
  def withKalixOptions(opts: Option[kalix.MethodOptions]): KalixMethod =
    opts match {
      case Some(methodOptions) => withKalixOptions(methodOptions)
      case None                => this
    }

  private[kalix] def mergeKalixOptions(
      source: Option[kalix.MethodOptions],
      addOn: kalix.MethodOptions): kalix.MethodOptions = {
    val builder = source match {
      case Some(src) => src.toBuilder
      case None      => kalix.MethodOptions.newBuilder()
    }
    builder.mergeFrom(addOn)
    builder.build()
  }
}

trait ExtractorCreator {
  def apply(descriptor: Descriptors.Descriptor): ParameterExtractor[DynamicMessageContext, AnyRef]
}

/**
 * Ensures all generated names in a given package are unique, noting that grpcMethod names and message names must not
 * conflict.
 *
 * Note that it is important to make sure that invoking this is done in an deterministic order or else JVMs on different
 * nodes will generate different names for the same method. Sorting can be done using ReflectionUtils.methodOrdering
 */
class NameGenerator {
  private var names: Set[String] = Set.empty

  def getName(base: String): String = {
    if (names(base)) {
      incrementName(base, 1)
    } else {
      names += base
      base
    }
  }

  @tailrec
  private def incrementName(base: String, inc: Int): String = {
    val name = base + inc
    if (names(name)) {
      incrementName(base, inc + 1)
    } else {
      names += name
      name
    }
  }
}
