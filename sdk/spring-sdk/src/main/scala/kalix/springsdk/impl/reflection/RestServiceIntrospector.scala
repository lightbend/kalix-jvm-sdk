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

import java.lang.annotation.Annotation
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Method

import scala.reflect.ClassTag

import kalix.springsdk.impl.path.SpringPathPattern
import kalix.springsdk.impl.path.SpringPathPatternParser
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.annotation.SynthesizingMethodParameter
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.MethodParameter
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam

object RestServiceIntrospector {

  private val discoverer = new DefaultParameterNameDiscoverer

  def inspectService[T](component: Class[T]): RestService = {
    // Spring defines a number of annotations, eg, @GetMapping, @PostMapping, etc, that we want to support. These
    // annotations are annotated with @AliasFor annotations that essentially map all their properties to the more
    // generic @RequestMapping annotation. Spring's annotation support understands these annotations, and is therefore
    // able to, when we request reading the RequestMapping annotation, read all of these so called sub-annotations, and
    // merge their properties according to the @AliasFor configuration, so that we only have to do the below call to
    // read the entire hierarchy.

    val classMapping = Option(AnnotatedElementUtils.findMergedAnnotation(component, classOf[RequestMapping]))
    classMapping.foreach { mapping =>
      validateRequestMapping(component, mapping)
    }

    val methodMappings = component.getMethods
      .map { method =>
        method -> Option(AnnotatedElementUtils.findMergedAnnotation(method, classOf[RequestMapping]))
      }
      .collect { case (method, Some(mapping)) =>
        // extract out @PathVariable, @RequestParameter, @RequestHeader and @RequestBody parameters (using the same
        // utility method above).
        // Spring has a nice wrapper around parameters that we will use. Note, we use SynthesizingMethodParameter
        // over MethodParameter because the former will resolve @AliasFor annotations for us.
        val params = for (i <- 0 until method.getParameterCount) yield {
          val param = new SynthesizingMethodParameter(method, i)
          // Allows using parameter names
          param.initParameterNameDiscovery(discoverer)
          param
        }
        val restParams = params.map(inspectParameter)
        RestMethod(classMapping, mapping, method, restParams)
      }

    RestService(methodMappings)
  }

  // This method may become more dynamic, eg, certain component types may define their own annotations for parameters,
  // and so they'll need their own conversions.
  def inspectParameter(parameter: MethodParameter): RestMethodParameter = parameter match {
    case PathVariableMatcher(pathVariable)   => PathParameter(parameter, pathVariable)
    case RequestParamMatcher(requestParam)   => QueryParamParameter(parameter, requestParam)
    case RequestHeaderMatcher(requestHeader) => HeaderParameter(parameter, requestHeader)
    case RequestBodyMatcher(requestBody)     => BodyParameter(parameter, requestBody)
    case _                                   => UnhandledParameter(parameter)
  }

  private class AnnotationMatcher[T <: Annotation: ClassTag] {
    def unapply(parameter: MethodParameter): Option[T] =
      Option(parameter.getParameterAnnotation[T](implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]))
  }
  private object PathVariableMatcher extends AnnotationMatcher[PathVariable]
  private object RequestParamMatcher extends AnnotationMatcher[RequestParam]
  private object RequestHeaderMatcher extends AnnotationMatcher[RequestHeader]
  private object RequestBodyMatcher extends AnnotationMatcher[RequestBody]

  sealed trait RestMethodParameter {
    def param: MethodParameter
  }

  case class UnhandledParameter(param: MethodParameter) extends RestMethodParameter
  case class PathParameter(param: MethodParameter, annotation: PathVariable) extends RestMethodParameter {
    val name: String = if (annotation.name().nonEmpty) {
      annotation.name()
    } else {
      param.getParameterName
    }
  }
  case class QueryParamParameter(param: MethodParameter, annotation: RequestParam) extends RestMethodParameter {
    val name: String = if (annotation.name().nonEmpty) {
      annotation.name()
    } else {
      param.getParameterName
    }
  }
  case class HeaderParameter(param: MethodParameter, annotation: RequestHeader) extends RestMethodParameter {
    val name: String = if (annotation.name().nonEmpty) {
      annotation.name()
    } else {
      param.getParameterName
    }
  }
  case class BodyParameter(param: MethodParameter, annotation: RequestBody) extends RestMethodParameter

  case class RestService(methods: Seq[RestMethod])

  case class RestMethod(
      classMapping: Option[RequestMapping],
      mapping: RequestMapping,
      javaMethod: Method,
      params: Seq[RestMethodParameter]) {

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

    val path: String = {
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

    val parsedPath: SpringPathPattern = SpringPathPatternParser.parse(path)

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

  private def validateRequestMapping(element: AnnotatedElement, mapping: RequestMapping): Unit = {
    if (!isEmpty(mapping.consumes())) {
      throw new ServiceIntrospectionException(
        element,
        "Unsupported RequestMapping attribute: consumes. Kalix Spring SDK does not support mapping requests by consumes, all methods are assumed to handle JSON and only JSON.")
    }
    if (!isEmpty(mapping.produces())) {
      throw new ServiceIntrospectionException(
        element,
        "Unsupported RequestMapping attribute: produces. Kalix Spring SDK does not support mapping requests by what it produces, all methods are assumed to produce JSON and only JSON.")
    }
    if (!isEmpty(mapping.headers())) {
      throw new ServiceIntrospectionException(
        element,
        "Unsupported RequestMapping attribute: headers. Kalix Spring SDK does not support mapping requests by headers.")
    }
    if (!isEmpty(mapping.params())) {
      throw new ServiceIntrospectionException(
        element,
        "Unsupported RequestMapping attribute: params. Kalix Spring SDK does not support mapping requests by request parameters.")
    }
    // This could be relaxed, since gRPC transcoding does have an additionalBindings field.
    if (!isEmpty(mapping.path()) && mapping.path().length > 1) {
      throw new ServiceIntrospectionException(
        element,
        "Invalid multiple path mapping. Kalix Spring SDK only supports mapping methods to one HTTP request path.")
    }
    if (!isEmpty(mapping.method()) && mapping.method().length > 1) {
      throw new ServiceIntrospectionException(
        element,
        "Invalid multiple request method mapping. Kalix Spring SDK only supports mapping methods to one HTTP request method.")
    }
  }

  private def isEmpty[T](array: Array[T]): Boolean = array == null || array.isEmpty

}

class ServiceIntrospectionException(element: AnnotatedElement, msg: String) extends RuntimeException(msg)
