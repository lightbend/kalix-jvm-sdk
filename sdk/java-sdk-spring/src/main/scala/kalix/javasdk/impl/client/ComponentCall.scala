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
import org.springframework.util.MultiValueMap
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.util.UriComponentsBuilder
import java.lang.reflect.Method

import scala.jdk.CollectionConverters._
import java.lang.reflect.ParameterizedType
import java.util.Optional

import scala.jdk.OptionConverters._

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ComponentCall[A1, R](kalixClient: KalixClient, lambda: scala.Any, entityId: Optional[String]) { //TODO rename for workflows
  def params(a1: A1): DeferredCall[Any, R] = {
    ComponentCall.invoke(Seq(a1), kalixClient, lambda, entityId.toScala);
  }
}

object ComponentCall {

  private val logger = LoggerFactory.getLogger(classOf[ComponentCall.type])

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

    val uriStr = buildUriString(restMethod.parsedPath.path, queryParams, pathVariables)
    val kalixClientImpl = kalixClient.asInstanceOf[RestKalixClientImpl]

    logger.info(s"Running $requestMethod $uriStr")

    requestMethod match {
      case RequestMethod.GET     => kalixClientImpl.get(uriStr, returnType)
      case RequestMethod.HEAD    => ???
      case RequestMethod.POST    => kalixClientImpl.runPost(uriStr, body, returnType)
      case RequestMethod.PUT     => ???
      case RequestMethod.PATCH   => kalixClientImpl.runPatch(uriStr, body, returnType)
      case RequestMethod.DELETE  => kalixClient.delete(uriStr, returnType)
      case RequestMethod.OPTIONS => ???
      case RequestMethod.TRACE   => ???
    }
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
  private def buildUriString(
      path: String,
      queryParams: MultiValueMap[String, String],
      pathVariables: Map[String, String]): String = {
    val uri = UriComponentsBuilder
      .newInstance() //TODO not sure if this a correct builder, Spring uses DefaultUriBuilder, but this one is private
      .path(path)
      .queryParams(queryParams)
      .build(pathVariables.asJava)

    if (uri.getQuery != null) {
      uri.getPath + "?" + uri.getQuery //TODO we should we should use URI or Uri everywhere translation to/from makes no sense
    } else {
      uri.getPath
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
