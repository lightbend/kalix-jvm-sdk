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

import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletionStage
import java.util.function.Function

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.Uri
import com.google.protobuf.Descriptors
import com.google.protobuf.DynamicMessage
import com.google.protobuf.any.Any
import kalix.javasdk.DeferredCall
import kalix.javasdk.DeferredCallResponseException
import kalix.javasdk.StatusCode.ErrorCode
import kalix.javasdk.impl.AnySupport
import kalix.javasdk.impl.JsonMessageCodec
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.impl.RestDeferredCall
import kalix.javasdk.impl.http.HttpEndpointMethodDefinition
import kalix.javasdk.impl.http.HttpEndpointMethodDefinition.ANY_METHOD
import kalix.spring.KalixClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder
import org.springframework.web.util.UriComponentsBuilder

/**
 * INTERNAL API
 */
final class RestKalixClientImpl(messageCodec: JsonMessageCodec) extends KalixClient {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private var services: Seq[HttpEndpointMethodDefinition] = Seq.empty

  // At the time of creation, Proxy Discovery has not happened yet
  // and we need the ProxyInfo to build the WebClient, so we need a Promise[WebClient]
  private val promisedWebClient: Promise[WebClient] = Promise[WebClient]()

  def setWebClient(localWebClient: WebClient) = {
    if (!promisedWebClient.isCompleted) promisedWebClient.trySuccess(localWebClient)
  }

  private val webClient: Future[WebClient] = promisedWebClient.future

  def registerComponent(descriptor: Descriptors.ServiceDescriptor): Unit = {
    services ++= HttpEndpointMethodDefinition.extractForService(descriptor)
  }

  private def buildWrappedBody[P](
      httpDef: HttpEndpointMethodDefinition,
      inputBuilder: DynamicMessage.Builder,
      body: Option[P] = None): Any = {
    if (body.isDefined && httpDef.rule.body.nonEmpty) {
      val bodyField = httpDef.methodDescriptor.getInputType.getFields.asScala
        .find(_.getName == httpDef.rule.body)
        .getOrElse(
          throw new IllegalArgumentException("Could not find a matching body field with name: " + httpDef.rule.body))

      inputBuilder.setField(bodyField, messageCodec.encodeJava(body.get))
    }
    Any(
      AnySupport.DefaultTypeUrlPrefix + "/" + inputBuilder.getDescriptorForType.getFullName,
      inputBuilder.build().toByteString)
  }

  override def get[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    runGet(uriStr, Map.empty, new LinkedMultiValueMap(), returnType)
  }

  def runGet[R](
      pathTemplate: String,
      pathVariables: Map[String, String],
      queryParams: MultiValueMap[String, String],
      returnType: Class[R]): DeferredCall[Any, R] = {

    val encodedPathVariables = pathVariables.view.mapValues(URLEncoder.encode(_, StandardCharsets.UTF_8)).toMap
    val uri = buildUri(pathTemplate, queryParams, encodedPathVariables)(UriComponentsBuilder.newInstance())
    val akkaUri = toUri(uri) //TODO this should be replaced with queryParams and pathVariables

    logger.trace(s"Composing HTTP [GET] request [$uri]")

    matchMethodOrThrow(HttpMethods.GET, akkaUri.path) { httpDef =>
      requestToRestDefCall(
        akkaUri,
        body = None,
        httpDef,
        () =>
          webClient
            .flatMap(
              _.get()
                .uri(buildUri(pathTemplate, queryParams, pathVariables))
                .retrieve()
                .bodyToMono(returnType)
                .toFuture
                .asScala)
            .asJava)
    }
  }

  override def post[P, R](uriStr: String, body: P, returnType: Class[R]): DeferredCall[Any, R] = {
    runPost(uriStr, Map.empty, new LinkedMultiValueMap(), Some(body), returnType)
  }

  private def buildUri(
      path: String,
      queryParams: MultiValueMap[String, String],
      pathVariables: Map[String, String]): Function[UriBuilder, URI] = uriBuilder => {
    uriBuilder
      .path(path)
      .queryParams(queryParams)
      .build(pathVariables.asJava)
  }

  def runPost[R, P](
      pathTemplate: String,
      pathVariables: Map[String, String],
      queryParams: MultiValueMap[String, String],
      body: Option[P],
      returnType: Class[R]) = {

    val encodedPathVariables = pathVariables.view.mapValues(URLEncoder.encode(_, StandardCharsets.UTF_8)).toMap
    val uri = buildUri(pathTemplate, queryParams, encodedPathVariables)(UriComponentsBuilder.newInstance())
    val akkaUri = toUri(uri) //TODO this should be replaced with queryParams and pathVariables

    logger.trace(s"Composing HTTP [POST] request [$uri]")

    matchMethodOrThrow(HttpMethods.POST, akkaUri.path) { httpDef =>
      requestToRestDefCall(
        akkaUri,
        body,
        httpDef,
        () =>
          webClient.flatMap { client =>
            val requestBodySpec = client
              .post()
              .uri(buildUri(pathTemplate, queryParams, pathVariables))

            body.foreach(requestBodySpec.bodyValue)

            requestBodySpec
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  private def toUri[P, R](uri: URI): Uri = {
    if (uri.getRawQuery != null) {
      Uri(uri.getPath + "?" + uri.getRawQuery)
    } else {
      Uri(uri.getPath)
    }
  }

  override def post[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    runPost(uriStr, Map.empty, new LinkedMultiValueMap(), None, returnType)
  }

  override def put[P, R](uriStr: String, body: P, returnType: Class[R]): DeferredCall[Any, R] = {
    runPut(uriStr, Map.empty, new LinkedMultiValueMap(), Some(body), returnType)
  }

  def runPut[P, R](
      pathTemplate: String,
      pathVariables: Map[String, String],
      queryParams: MultiValueMap[String, String],
      body: Option[P],
      returnType: Class[R]): DeferredCall[Any, R] = {

    val encodedPathVariables = pathVariables.view.mapValues(URLEncoder.encode(_, StandardCharsets.UTF_8)).toMap
    val uri = buildUri(pathTemplate, queryParams, encodedPathVariables)(UriComponentsBuilder.newInstance())
    val akkaUri = toUri(uri) //TODO this should be replaced with queryParams and pathVariables

    logger.trace(s"Composing HTTP [PUT] request [$uri]")

    matchMethodOrThrow(HttpMethods.PUT, akkaUri.path) { httpDef =>
      requestToRestDefCall(
        akkaUri,
        body,
        httpDef,
        () =>
          webClient.flatMap { client =>
            val requestBodySpec = client
              .put()
              .uri(buildUri(pathTemplate, queryParams, pathVariables))

            body.foreach(requestBodySpec.bodyValue)

            requestBodySpec
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def put[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    runPut(uriStr, Map.empty, new LinkedMultiValueMap(), None, returnType)
  }

  override def patch[P, R](uriStr: String, body: P, returnType: Class[R]): DeferredCall[Any, R] = {
    runPatch(uriStr, Map.empty, new LinkedMultiValueMap(), Some(body), returnType)
  }

  def runPatch[R, P](
      pathTemplate: String,
      pathVariables: Map[String, String],
      queryParams: MultiValueMap[String, String],
      body: Option[P],
      returnType: Class[R]) = {

    val encodedPathVariables = pathVariables.view.mapValues(URLEncoder.encode(_, StandardCharsets.UTF_8)).toMap
    val uri = buildUri(pathTemplate, queryParams, encodedPathVariables)(UriComponentsBuilder.newInstance())
    val akkaUri = toUri(uri) //TODO this should be replaced with queryParams and pathVariables

    logger.trace(s"Composing HTTP [PATCH] request [$uri]")

    matchMethodOrThrow(HttpMethods.PATCH, akkaUri.path) { httpDef =>
      requestToRestDefCall(
        akkaUri,
        Some(body),
        httpDef,
        () =>
          webClient.flatMap { client =>
            val requestBodySpec = client
              .patch()
              .uri(buildUri(pathTemplate, queryParams, pathVariables))

            body.foreach(requestBodySpec.bodyValue)

            requestBodySpec
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def patch[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    runPatch(uriStr, Map.empty, new LinkedMultiValueMap(), None, returnType)
  }

  override def delete[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    runDelete(uriStr, Map.empty, new LinkedMultiValueMap(), returnType)
  }

  def runDelete[R](
      pathTemplate: String,
      pathVariables: Map[String, String],
      queryParams: MultiValueMap[String, String],
      returnType: Class[R]): DeferredCall[Any, R] = {

    val encodedPathVariables = pathVariables.view.mapValues(URLEncoder.encode(_, StandardCharsets.UTF_8)).toMap
    val uri = buildUri(pathTemplate, queryParams, encodedPathVariables)(UriComponentsBuilder.newInstance())
    val akkaUri = toUri(uri) //TODO this should be replaced with queryParams and pathVariables

    logger.trace(s"Composing HTTP [DELETE] request [$uri]")

    matchMethodOrThrow(HttpMethods.DELETE, akkaUri.path) { httpDef =>
      requestToRestDefCall(
        akkaUri,
        None,
        httpDef,
        () =>
          webClient.flatMap {
            _.delete()
              .uri(buildUri(pathTemplate, queryParams, pathVariables))
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  private def matchMethodOrThrow[R](httpMethod: HttpMethod, path: Uri.Path)(
      createDefCall: => HttpEndpointMethodDefinition => RestDeferredCall[Any, R]) = {
    services
      .find(d => (d.methodPattern == ANY_METHOD || httpMethod == d.methodPattern) && d.matches(path))
      .map {
        createDefCall(_)
      }
      .getOrElse(throw HttpMethodNotFoundException(httpMethod, path.toString()))
  }

  private def requestToRestDefCall[P, R](
      uri: Uri,
      body: Option[P],
      httpDef: HttpEndpointMethodDefinition,
      asyncCall: () => CompletionStage[R]): RestDeferredCall[Any, R] = {

    val inputBuilder = DynamicMessage.newBuilder(httpDef.methodDescriptor.getInputType)

    httpDef.parsePathParametersInto(uri.path, inputBuilder)
    httpDef.parseRequestParametersInto(uri.query().toMultiMap, inputBuilder)

    val wrappedBody = buildWrappedBody(httpDef, inputBuilder, body)

    RestDeferredCall[Any, R](
      message = wrappedBody,
      metadata = MetadataImpl.Empty,
      fullServiceName = httpDef.methodDescriptor.getService.getFullName,
      methodName = httpDef.methodDescriptor.getName,
      asyncCall = () =>
        asyncCall().exceptionally {
          case responseException: WebClientResponseException =>
            throw DeferredCallResponseException(
              responseException.getMessage,
              fromWebClientResponse(responseException),
              responseException)
          case other: Throwable => throw other
        })
  }

  private def fromWebClientResponse(webClientResponseException: WebClientResponseException): ErrorCode = {
    webClientResponseException match {
      case _: WebClientResponseException.NotFound            => ErrorCode.NOT_FOUND
      case _: WebClientResponseException.BadRequest          => ErrorCode.BAD_REQUEST
      case _: WebClientResponseException.Conflict            => ErrorCode.CONFLICT
      case _: WebClientResponseException.Forbidden           => ErrorCode.FORBIDDEN
      case _: WebClientResponseException.Unauthorized        => ErrorCode.UNAUTHORIZED
      case _: WebClientResponseException.GatewayTimeout      => ErrorCode.GATEWAY_TIMEOUT
      case _: WebClientResponseException.ServiceUnavailable  => ErrorCode.SERVICE_UNAVAILABLE
      case _: WebClientResponseException.TooManyRequests     => ErrorCode.TOO_MANY_REQUESTS
      case _: WebClientResponseException.InternalServerError => ErrorCode.INTERNAL_SERVER_ERROR
      case _                                                 => ErrorCode.INTERNAL_SERVER_ERROR
    }
  }

}

final case class HttpMethodNotFoundException(httpMethod: HttpMethod, uriStr: String)
    extends RuntimeException(s"No matching service for method=$httpMethod path=$uriStr")
