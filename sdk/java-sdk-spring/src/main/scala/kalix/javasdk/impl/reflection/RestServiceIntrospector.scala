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

import java.lang.annotation.Annotation
import java.lang.reflect.AnnotatedElement

import scala.reflect.ClassTag

// TODO: abstract away spring dependency
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.MethodParameter
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.annotation.SynthesizingMethodParameter
import org.springframework.web.bind.annotation._

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

    import ReflectionUtils.methodOrdering
    val methodMappings = component.getMethods.sorted // make sure we get the methods in deterministic order
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
        SyntheticRequestServiceMethod(classMapping, mapping, method, restParams)
      }

    RestService(methodMappings.toIndexedSeq)
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

  sealed trait RestNamedMethodParameter extends RestMethodParameter {
    def name: String
  }

  case class UnhandledParameter(param: MethodParameter) extends RestMethodParameter
  case class PathParameter(param: MethodParameter, annotation: PathVariable) extends RestNamedMethodParameter {
    val name: String = if (annotation.name().nonEmpty) {
      annotation.name()
    } else {
      param.getParameterName
    }
  }
  case class QueryParamParameter(param: MethodParameter, annotation: RequestParam) extends RestNamedMethodParameter {
    val name: String = if (annotation.name().nonEmpty) {
      annotation.name()
    } else {
      param.getParameterName
    }
  }
  case class HeaderParameter(param: MethodParameter, annotation: RequestHeader) extends RestNamedMethodParameter {
    val name: String = if (annotation.name().nonEmpty) {
      annotation.name()
    } else {
      param.getParameterName
    }
  }
  case class BodyParameter(param: MethodParameter, annotation: RequestBody) extends RestMethodParameter

  case class RestService(methods: Seq[SyntheticRequestServiceMethod])

  private[kalix] def validateRequestMapping(element: AnnotatedElement, mapping: RequestMapping): Unit = {
    if (!isEmpty(mapping.consumes())) {
      throw ServiceIntrospectionException(
        element,
        "Unsupported RequestMapping attribute: consumes. Kalix Java SDK does not support mapping requests by consumes, all methods are assumed to handle JSON and only JSON.")
    }
    if (!isEmpty(mapping.produces())) {
      throw ServiceIntrospectionException(
        element,
        "Unsupported RequestMapping attribute: produces. Kalix Java SDK does not support mapping requests by what it produces, all methods are assumed to produce JSON and only JSON.")
    }
    if (!isEmpty(mapping.headers())) {
      throw ServiceIntrospectionException(
        element,
        "Unsupported RequestMapping attribute: headers. Kalix Java SDK does not support mapping requests by headers.")
    }
    if (!isEmpty(mapping.params())) {
      throw ServiceIntrospectionException(
        element,
        "Unsupported RequestMapping attribute: params. Kalix Java SDK does not support mapping requests by request parameters.")
    }
    // This could be relaxed, since gRPC transcoding does have an additionalBindings field.
    if (!isEmpty(mapping.path()) && mapping.path().length > 1) {
      throw ServiceIntrospectionException(
        element,
        "Invalid multiple path mapping. Kalix Java SDK only supports mapping methods to one HTTP request path.")
    }
    if (!isEmpty(mapping.method()) && mapping.method().length > 1) {
      throw ServiceIntrospectionException(
        element,
        "Invalid multiple request method mapping. Kalix Java SDK only supports mapping methods to one HTTP request method.")
    }
  }

  private[kalix] def isEmpty[T](array: Array[T]): Boolean = array == null || array.isEmpty

}
