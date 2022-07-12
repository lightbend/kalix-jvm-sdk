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

package kalix.springsdk.impl.reflection

import com.google.protobuf.Descriptors
import kalix.springsdk.impl.path.{ PathPattern, PathPatternParser }
import kalix.springsdk.impl.reflection.RestServiceIntrospector.{
  isEmpty,
  validateRequestMapping,
  PathParameter,
  RestMethodParameter
}
import org.springframework.web.bind.annotation.{ RequestMapping, RequestMethod }

import java.lang.reflect.Method
import scala.annotation.tailrec

sealed trait ServiceMethod {
  def methodName: String
  def javaMethodOpt: Option[Method]

  def requestMethod: RequestMethod
  def pathTemplate: String
}

sealed trait AnyServiceMethod extends ServiceMethod {
  def inputType: Class[_]

  protected def buildPathTemplate(componentName: String, methodName: String): String = {
    val cls = componentName.replace("$", ".")
    s"/$cls/${methodName.capitalize}" // FIXME: must pass through NameGenerator?
  }
}

/**
 * Build from methods annotated with @Subscription at type level.
 *
 * It's used as a 'virtual' method because there is not Java method backing it. It will exist only in the gRPC
 * descriptor and will be used for view updates with transform = false
 */
case class VirtualServiceMethod(component: Class[_], methodName: String, inputType: Class[_]) extends AnyServiceMethod {

  override def requestMethod: RequestMethod = RequestMethod.POST

  override def javaMethodOpt: Option[Method] = None

  val pathTemplate = buildPathTemplate(component.getName, methodName)
}

/**
 * Build from methods annotated with @Subscription. Those methods are not annotated with Spring REST annotations, but
 * they become a REST method at the end.
 */
case class RestServiceMethod(javaMethod: Method) extends AnyServiceMethod {

  val inputType: Class[_] = javaMethod.getParameterTypes()(0)

  override def methodName: String = javaMethod.getName

  override def requestMethod: RequestMethod = RequestMethod.POST
  override def javaMethodOpt: Option[Method] = Some(javaMethod)

  val pathTemplate = buildPathTemplate(javaMethod.getDeclaringClass.getName, methodName)
}

/**
 * Build from Spring annotations
 *
 * @param callable
 *   Is this actually a method that will ever be called or just there for metadata annotations?
 */
case class SpringRestServiceMethod(
    classMapping: Option[RequestMapping],
    mapping: RequestMapping,
    javaMethod: Method,
    params: Seq[RestMethodParameter],
    callable: Boolean = true)
    extends ServiceMethod {

  override def javaMethodOpt: Option[Method] = Some(javaMethod)
  override def methodName: String = javaMethod.getName
  val requestProtoMessageName: String = javaMethod.getName.capitalize + "Request"

  // First fail on unsupported mapping values. Should all default to empty arrays, but let's not trust that
  {
    validateRequestMapping(javaMethod, mapping)
    if (!isEmpty(mapping.method()) && classMapping.exists(cm => !isEmpty(cm.method()))) {
      throw new ServiceIntrospectionException(
        javaMethod,
        "Invalid request method mapping. A request method mapping may only be defined on the class, or on the method, but not both.")
    }
    if (isEmpty(mapping.path()) && classMapping.forall(cm => isEmpty(cm.path()))) {
      throw new ServiceIntrospectionException(
        javaMethod,
        "Missing path mapping. Kalix Spring SDK methods must have a path defined.")
    }
    if (isEmpty(mapping.method()) && classMapping.forall(cm => isEmpty(cm.method()))) {
      throw new ServiceIntrospectionException(
        javaMethod,
        "Missing request method mapping. Kalix Spring SDK methods must have a request method defined.")
    }
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

  override def pathTemplate: String = parsedPath.toGrpcTranscodingPattern

  val pathParameters: Seq[PathParameter] = params.collect { case p: PathParameter => p }

  {
    // Validate all the path parameters exist
    pathParameters.foreach { param =>
      if (!parsedPath.fields.contains(param.name)) {
        throw new ServiceIntrospectionException(
          param.param.getAnnotatedElement,
          s"There is no parameter named ${param.name} in the path pattern for this method.")
      }
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

case class KalixMethod(
    serviceMethod: ServiceMethod,
    methodOptions: Seq[kalix.MethodOptions] = Seq.empty,
    entityKeys: Seq[String] = Seq.empty) {

  def withKalixOptions(opts: kalix.MethodOptions): KalixMethod =
    copy(methodOptions = methodOptions :+ opts)
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
