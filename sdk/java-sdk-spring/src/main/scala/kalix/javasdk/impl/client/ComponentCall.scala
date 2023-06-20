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
import java.util.Optional

import scala.jdk.OptionConverters._

import com.google.protobuf.any.Any
import kalix.javasdk.DeferredCall
import kalix.javasdk.annotations.EntityType
import kalix.javasdk.annotations.GenerateEntityKey
import kalix.javasdk.impl.reflection.EntityKeyExtractor
import kalix.javasdk.impl.reflection.RestServiceIntrospector
import kalix.javasdk.impl.reflection.RestServiceIntrospector.BodyParameter
import kalix.javasdk.impl.reflection.RestServiceIntrospector.PathParameter
import kalix.javasdk.impl.reflection.RestServiceIntrospector.QueryParamParameter
import kalix.javasdk.impl.reflection.RestServiceIntrospector.RestService
import kalix.javasdk.impl.reflection.SyntheticRequestServiceMethod
import kalix.spring.KalixClient
import kalix.spring.impl.RestKalixClientImpl
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.bind.annotation.RequestMethod

class ComponentCall[A1, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) { //TODO rename for workflows
  def params(a1: A1): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1), kalixClient, lambda, entityId.toScala);
  }
}

object ComponentCall {

  def noParams[R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]): DeferredCall[Any, R] = {
    invoke(Seq.empty, kalixClient, lambda, entityId.toScala);
  }

  private[client] def invoke[R](
      params: Seq[scala.Any],
      kalixClient: KalixClient,
      lambda: scala.Any,
      entityId: Option[String]): DeferredCall[Any, R] = {

    val method = MethodRefResolver.resolveMethodRef(lambda)
    val declaringClass = method.getDeclaringClass

    val returnType =
      method.getGenericReturnType.asInstanceOf[ParameterizedType].getActualTypeArguments.head.asInstanceOf[Class[R]]

    val restService: RestService = RestServiceIntrospector.inspectService(declaringClass)
    val restMethod: SyntheticRequestServiceMethod =
      restService.methods.find(_.javaMethod.getName == method.getName).get

    val requestMethod: RequestMethod = restMethod.requestMethod

    val queryParams: LinkedMultiValueMap[String, String] = new LinkedMultiValueMap()
    restMethod.params
      .collect { case p: QueryParamParameter => p }
      .foreach(param => {
        queryParams.add(param.name, params(param.param.getParameterIndex).toString) //TODO null?
      })

    val pathVariables: Map[String, String] = restMethod.params
      .collect { case p: PathParameter => p }
      .map(param => (param.name, params(param.param.getParameterIndex).toString))
      .toMap ++ entityIdVariables(entityId, method)

    val bodyIndex = restMethod.params.collect { case p: BodyParameter => p }.map(_.param.getParameterIndex).headOption
    val body = bodyIndex.map(params(_))

    val kalixClientImpl = kalixClient.asInstanceOf[RestKalixClientImpl]

    val pathTemplate = restMethod.parsedPath.path

    requestMethod match {
      case RequestMethod.GET     => kalixClientImpl.runGet(pathTemplate, pathVariables, queryParams, returnType)
      case RequestMethod.HEAD    => notSupported(requestMethod, pathTemplate)
      case RequestMethod.POST    => kalixClientImpl.runPost(pathTemplate, pathVariables, queryParams, body, returnType)
      case RequestMethod.PUT     => kalixClientImpl.runPut(pathTemplate, pathVariables, queryParams, body, returnType)
      case RequestMethod.PATCH   => kalixClientImpl.runPatch(pathTemplate, pathVariables, queryParams, body, returnType)
      case RequestMethod.DELETE  => kalixClientImpl.runDelete(pathTemplate, pathVariables, queryParams, returnType)
      case RequestMethod.OPTIONS => notSupported(requestMethod, pathTemplate)
      case RequestMethod.TRACE   => notSupported(requestMethod, pathTemplate)
    }
  }

  private def notSupported[R](requestMethod: RequestMethod, pathTemplate: String) = {
    throw new IllegalStateException(s"HTTP $requestMethod not supported when calling $pathTemplate")
  }

  private def entityIdVariables[R](entityId: Option[String], method: Method): Map[String, String] = {

    val declaringClass = method.getDeclaringClass
    if (declaringClass.getAnnotation(classOf[EntityType]) == null) {
      //not an entity
      Map.empty
    } else {
      val entityKeys = EntityKeyExtractor.extractEntityKeys(declaringClass, method)
      val generateEntityKey = method.getAnnotation(classOf[GenerateEntityKey])

      if (generateEntityKey != null) {
        Map.empty
      } else {
        entityId match {
          case Some(value) => Map(entityKeys.head -> value) //TODO handle compound keys
          case None        => throw new IllegalStateException(s"Entity id is missing while calling ${method.getName}")
        }
      }
    }
  }
}

class ComponentCall2[A1, A2, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def invoke(a1: A1, a2: A2): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2), kalixClient, lambda, entityId.toScala)
  }
}

class ComponentCall3[A1, A2, A3, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) {

  def invoke(a1: A1, a2: A2, a3: A3): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1, a2, a3), kalixClient, lambda, entityId.toScala)
  }
}
