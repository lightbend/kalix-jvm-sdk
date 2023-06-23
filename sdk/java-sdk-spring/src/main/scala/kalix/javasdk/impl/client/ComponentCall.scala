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

package kalix.javasdk.impl.client

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.util
import java.util.Optional

import scala.jdk.OptionConverters._

import akka.http.scaladsl.model.HttpMethods
import com.google.protobuf.any.Any
import kalix.javasdk.DeferredCall
import kalix.javasdk.annotations.EntityType
import kalix.javasdk.annotations.TypeId
import kalix.javasdk.impl.reflection.IdExtractor
import kalix.javasdk.impl.reflection.RestServiceIntrospector
import kalix.javasdk.impl.reflection.RestServiceIntrospector.BodyParameter
import kalix.javasdk.impl.reflection.RestServiceIntrospector.PathParameter
import kalix.javasdk.impl.reflection.RestServiceIntrospector.QueryParamParameter
import kalix.javasdk.impl.reflection.RestServiceIntrospector.RestService
import kalix.javasdk.impl.reflection.SyntheticRequestServiceMethod
import kalix.spring.KalixClient
import kalix.spring.impl.RestKalixClientImpl
import org.springframework.web.bind.annotation.RequestMethod

final class ComponentCall[A1, R](kalixClient: KalixClient, lambda: scala.Any, id: Optional[String]) {
  def params(a1: A1): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1), kalixClient, lambda, id.toScala)
  }
}

object ComponentCall {

  def noParams[R](kalixClient: KalixClient, lambda: scala.Any, id: Optional[String]): DeferredCall[Any, R] = {
    invoke(Seq.empty, kalixClient, lambda, id.toScala)
  }

  private[client] def invoke[R](
      params: Seq[scala.Any],
      kalixClient: KalixClient,
      lambda: scala.Any,
      id: Option[String]): DeferredCall[Any, R] = {

    val method = MethodRefResolver.resolveMethodRef(lambda)
    val declaringClass = method.getDeclaringClass

    val returnType =
      method.getGenericReturnType.asInstanceOf[ParameterizedType].getActualTypeArguments.head.asInstanceOf[Class[R]]

    val restService: RestService = RestServiceIntrospector.inspectService(declaringClass)
    val restMethod: SyntheticRequestServiceMethod =
      restService.methods.find(_.javaMethod.getName == method.getName).get

    val requestMethod: RequestMethod = restMethod.requestMethod

    val queryParams: Map[String, util.List[scala.Any]] = restMethod.params
      .collect { case p: QueryParamParameter => p }
      .map(p => (p.name, getQueryParam(params, p.param.getParameterIndex)))
      .toMap

    val pathVariables: Map[String, ?] = restMethod.params
      .collect { case p: PathParameter => p }
      .map(p => (p.name, getPathParam(params, p.param.getParameterIndex, p.name)))
      .toMap ++ idVariables(id, method)

    val bodyIndex = restMethod.params.collect { case p: BodyParameter => p }.map(_.param.getParameterIndex).headOption
    val body = bodyIndex.map(params(_))

    val kalixClientImpl = kalixClient.asInstanceOf[RestKalixClientImpl]

    val pathTemplate = restMethod.parsedPath.path

    requestMethod match {
      case RequestMethod.GET =>
        kalixClientImpl.runWithoutBody(HttpMethods.GET, pathTemplate, pathVariables, queryParams, returnType)
      case RequestMethod.HEAD => notSupported(requestMethod, pathTemplate)
      case RequestMethod.POST =>
        kalixClientImpl.runWithBody(HttpMethods.POST, pathTemplate, pathVariables, queryParams, body, returnType)
      case RequestMethod.PUT =>
        kalixClientImpl.runWithBody(HttpMethods.PUT, pathTemplate, pathVariables, queryParams, body, returnType)
      case RequestMethod.PATCH =>
        kalixClientImpl.runWithBody(HttpMethods.PATCH, pathTemplate, pathVariables, queryParams, body, returnType)
      case RequestMethod.DELETE =>
        kalixClientImpl.runWithoutBody(HttpMethods.DELETE, pathTemplate, pathVariables, queryParams, returnType)
      case RequestMethod.OPTIONS => notSupported(requestMethod, pathTemplate)
      case RequestMethod.TRACE   => notSupported(requestMethod, pathTemplate)
    }
  }

  private def getQueryParam(params: Seq[scala.Any], parameterIndex: Int): util.List[scala.Any] = {
    val value = params(parameterIndex)
    if (value == null) {
      util.List.of()
    } else if (value.isInstanceOf[util.List[_]]) {
      value.asInstanceOf[util.List[scala.Any]]
    } else {
      util.List.of(value)
    }
  }

  private def getPathParam(params: Seq[scala.Any], parameterIndex: Int, paramName: String): scala.Any = {
    val value = params(parameterIndex)
    if (value == null) {
      throw new IllegalStateException(s"Path param [$paramName] cannot be null.")
    }
    value
  }

  private def notSupported(requestMethod: RequestMethod, pathTemplate: String) = {
    throw new IllegalStateException(s"HTTP $requestMethod not supported when calling $pathTemplate")
  }

  private def idVariables(id: Option[String], method: Method): Map[String, String] = {

    val declaringClass = method.getDeclaringClass
    if (declaringClass.getAnnotation(classOf[EntityType]) == null &&
      declaringClass.getAnnotation(classOf[TypeId]) == null) {
      //not an entity or workflows
      Map.empty
    } else if (IdExtractor.shouldGenerateId(method)) {
      Map.empty
    } else {
      val idNames = IdExtractor.extractIds(declaringClass, method)
      id match {
        case Some(value) => Map(idNames.head -> value) //TODO handle compound keys
        case None        => throw new IllegalStateException(s"Id is missing while calling ${method.getName}")
      }
    }
  }
}

// format: off
final class ComponentCall2[A1, A2, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall3[A1, A2, A3, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall4[A1, A2, A3, A4, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall5[A1, A2, A3, A4, A5, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall6[A1, A2, A3, A4, A5, A6, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall7[A1, A2, A3, A4, A5, A6, A7, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall8[A1, A2, A3, A4, A5, A6, A7, A8, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall9[A1, A2, A3, A4, A5, A6, A7, A8, A9, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall10[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall11[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall12[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall13[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall14[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall15[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall16[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall17[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall18[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall19[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall20[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20), kalixClient, lambda, entityId.toScala)
  }
}
final class ComponentCall21[A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def params(a1: A1, a2: A2, a3: A3, a4: A4, a5: A5, a6: A6, a7: A7, a8: A8, a9: A9, a10: A10, a11: A11, a12: A12, a13: A13, a14: A14, a15: A15, a16: A16, a17: A17, a18: A18, a19: A19, a20: A20, a21: A21): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, a16, a17, a18, a19, a20, a21), kalixClient, lambda, entityId.toScala)
  }
}
// format: on
