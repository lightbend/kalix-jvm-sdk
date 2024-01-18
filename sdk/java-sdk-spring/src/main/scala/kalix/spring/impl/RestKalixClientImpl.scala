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

import java.net.URI
import java.util
import java.util.Optional
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
import kalix.javasdk.HttpResponse
import kalix.javasdk.Metadata
import kalix.javasdk.StatusCode
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
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriBuilder

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
    runGet(uriStr, returnType)
  }

  private def runGet[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    val uri = Uri(uriStr)
    matchMethodOrThrow(HttpMethods.GET, uri.path.toString()) { httpDef =>
      requestToRestDefCall(
        uri,
        body = None,
        httpDef,
        (metadata: Metadata) =>
          webClient.flatMap { client =>
            val spec = client
              .get()
              .uri(uriStr)
              .asInstanceOf[RequestHeadersUriSpec[_]]

            addHeaders(metadata, spec)

            spec
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def post[P, R](uriStr: String, body: P, returnType: Class[R]): DeferredCall[Any, R] = {
    runPost(uriStr, Some(body), returnType)
  }

  private def buildUri(
      path: String,
      pathVariables: Map[String, scala.Any],
      queryParams: Map[String, util.List[scala.Any]]): Function[UriBuilder, URI] = uriBuilder => {
    val builder = uriBuilder
      .path(path)
    queryParams.map { case (name, values) =>
      builder.queryParam(name, values)
    }
    builder.build(pathVariables.asJava)
  }

  private def runPost[R, P](uriStr: String, body: Option[P], returnType: Class[R]) = {
    val akkaUri = Uri(uriStr)
    matchMethodOrThrow(HttpMethods.POST, akkaUri.path.toString()) { httpDef =>
      requestToRestDefCall(
        akkaUri,
        body,
        httpDef,
        (metadata: Metadata) =>
          webClient.flatMap { client =>
            val requestBodySpec = client
              .post()
              .uri(uriStr)

            body.foreach(requestBodySpec.bodyValue)

            addHeaders(metadata, requestBodySpec)

            requestBodySpec
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  private def addHeaders[P, R](metadata: Metadata, spec: RequestHeadersSpec[_]): Unit = {
    metadata.forEach(entry => {
      if (entry.isText) {
        spec.header(entry.getKey, entry.getValue)
      }
    })
  }

  override def post[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    runPost(uriStr, None, returnType)
  }

  override def put[P, R](uriStr: String, body: P, returnType: Class[R]): DeferredCall[Any, R] = {
    runPut(uriStr, Some(body), returnType)
  }

  private def runPut[P, R](uriStr: String, body: Option[P], returnType: Class[R]): DeferredCall[Any, R] = {
    val akkaUri = Uri(uriStr)
    matchMethodOrThrow(HttpMethods.PUT, akkaUri.path.toString()) { httpDef =>
      requestToRestDefCall(
        akkaUri,
        body,
        httpDef,
        (metadata: Metadata) =>
          webClient.flatMap { client =>
            val requestBodySpec = client
              .put()
              .uri(uriStr)

            body.foreach(requestBodySpec.bodyValue)

            addHeaders(metadata, requestBodySpec)

            requestBodySpec
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def put[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    runPut(uriStr, None, returnType)
  }

  override def patch[P, R](uriStr: String, body: P, returnType: Class[R]): DeferredCall[Any, R] = {
    runPatch(uriStr, Some(body), returnType)
  }

  private def runPatch[R, P](uriStr: String, body: Option[P], returnType: Class[R]) = {

    val akkaUri = Uri(uriStr)

    matchMethodOrThrow(HttpMethods.PATCH, akkaUri.path.toString()) { httpDef =>
      requestToRestDefCall(
        akkaUri,
        Some(body),
        httpDef,
        (metadata: Metadata) =>
          webClient.flatMap { client =>
            val requestBodySpec = client
              .patch()
              .uri(uriStr)

            body.foreach(requestBodySpec.bodyValue)

            addHeaders(metadata, requestBodySpec)

            requestBodySpec
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def patch[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    runPatch(uriStr, None, returnType)
  }

  override def delete[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    runDelete(uriStr, returnType)
  }

  private def runDelete[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    val akkaUri = Uri(uriStr)
    matchMethodOrThrow(HttpMethods.DELETE, akkaUri.path.toString()) { httpDef =>
      requestToRestDefCall(
        akkaUri,
        None,
        httpDef,
        (metadata: Metadata) =>
          webClient.flatMap { client =>
            val spec = client
              .delete()
              .uri(uriStr)
              .asInstanceOf[RequestHeadersUriSpec[_]]

            addHeaders(metadata, spec)

            spec
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  private[kalix] def runWithoutBody[R, P](
      httpMethod: HttpMethod,
      pathTemplate: String,
      pathVariables: Map[String, ?],
      queryParams: Map[String, util.List[scala.Any]],
      returnType: Class[R]): RestDeferredCall[Any, R] = {

    matchMethodOrThrow(httpMethod, pathTemplate) { httpDef =>
      typedRequestToRestDefCall(
        pathVariables,
        queryParams,
        None,
        httpDef,
        (metadata: Metadata) =>
          webClient.flatMap { client =>

            val requestBodySpec = requestHeadersUriSpec(client, httpMethod)
              .uri(buildUri(pathTemplate, pathVariables, queryParams))
              .asInstanceOf[RequestHeadersUriSpec[_]]

            addHeaders(metadata, requestBodySpec)

            if (returnType == classOf[HttpResponse]) {
              requestBodySpec
                .retrieve()
                .toEntity(classOf[Array[Byte]])
                .toFuture
                .asScala
                .map(toHttpResponse)
                .map(_.asInstanceOf[R])
            } else {
              requestBodySpec
                .retrieve()
                .bodyToMono(returnType)
                .toFuture
                .asScala
            }
          }.asJava)
    }
  }

  private def toHttpResponse[R](response: ResponseEntity[Array[Byte]]): HttpResponse = {
    val body = if (response.hasBody) {
      response.getBody
    } else {
      new Array[Byte](0)
    }
    val statusCode = StatusCode.Success.from(response.getStatusCode.value())
    val contentType = response.getHeaders.getFirst("Content-Type")
    HttpResponse.of(statusCode, contentType, body)
  }

  private[kalix] def runWithBody[R, P](
      httpMethod: HttpMethod,
      pathTemplate: String,
      pathVariables: Map[String, scala.Any],
      queryParams: Map[String, util.List[scala.Any]],
      body: Option[P],
      returnType: Class[R]): RestDeferredCall[Any, R] = {

    matchMethodOrThrow(httpMethod, pathTemplate) { httpDef =>
      typedRequestToRestDefCall(
        pathVariables,
        queryParams,
        body,
        httpDef,
        (metadata: Metadata) =>
          webClient.flatMap { client =>
            val requestBodySpec = requestBodyUriSpec(client, httpMethod)
              .uri(buildUri(pathTemplate, pathVariables, queryParams))

            body.foreach(requestBodySpec.bodyValue)

            addHeaders(metadata, requestBodySpec)

            if (returnType == classOf[HttpResponse]) {
              requestBodySpec
                .retrieve()
                .toEntity(classOf[Array[Byte]])
                .toFuture
                .asScala
                .map(toHttpResponse)
                .map(_.asInstanceOf[R])
            } else {
              requestBodySpec
                .retrieve()
                .bodyToMono(returnType)
                .toFuture
                .asScala
            }
          }.asJava)
    }
  }

  private def requestBodyUriSpec(client: WebClient, httpMethod: HttpMethod): WebClient.RequestBodyUriSpec = {
    httpMethod match {
      case HttpMethods.PUT   => client.put()
      case HttpMethods.POST  => client.post()
      case HttpMethods.PATCH => client.patch()
      case other => throw new IllegalStateException(s"RequestBodyUriSpec not supported for HTTP method [$other]")
    }
  }

  private def requestHeadersUriSpec(client: WebClient, httpMethod: HttpMethod): WebClient.RequestHeadersUriSpec[_] = {
    httpMethod match {
      case HttpMethods.GET    => client.get()
      case HttpMethods.DELETE => client.delete()
      case other => throw new IllegalStateException(s"RequestHeadersUriSpec not supported for HTTP method [$other]")
    }
  }

  private def typedRequestToRestDefCall[P, R](
      pathVariables: Map[String, scala.Any],
      queryParams: Map[String, util.List[scala.Any]],
      body: Option[P],
      httpDef: HttpEndpointMethodDefinition,
      asyncCall: Metadata => CompletionStage[R]): RestDeferredCall[Any, R] = {

    val inputBuilder = DynamicMessage.newBuilder(httpDef.methodDescriptor.getInputType)

    httpDef.parseTypedPathParametersInto(pathVariables, inputBuilder)
    httpDef.parseTypedRequestParametersInto(queryParams, inputBuilder)

    val wrappedBody = buildWrappedBody(httpDef, inputBuilder, body)

    RestDeferredCall[Any, R](
      message = wrappedBody,
      metadata = MetadataImpl.Empty,
      fullServiceName = httpDef.methodDescriptor.getService.getFullName,
      methodName = httpDef.methodDescriptor.getName,
      asyncCall = (metadata: Metadata) =>
        asyncCall(metadata).exceptionally {
          case responseException: WebClientResponseException =>
            throw DeferredCallResponseException(
              responseException.getMessage,
              fromWebClientResponse(responseException),
              responseException)
          case other: Throwable => throw other
        })
  }

  private def matchMethodOrThrow[R](httpMethod: HttpMethod, path: String)(
      createDefCall: => HttpEndpointMethodDefinition => RestDeferredCall[Any, R]) = {
    services
      .find(d => (d.methodPattern == ANY_METHOD || httpMethod == d.methodPattern) && d.matches(path))
      .map {
        createDefCall(_)
      }
      .getOrElse(throw HttpMethodNotFoundException(httpMethod, path))
  }

  private def requestToRestDefCall[P, R](
      uri: Uri,
      body: Option[P],
      httpDef: HttpEndpointMethodDefinition,
      asyncCall: Metadata => CompletionStage[R]): RestDeferredCall[Any, R] = {

    val inputBuilder = DynamicMessage.newBuilder(httpDef.methodDescriptor.getInputType)

    httpDef.parsePathParametersInto(uri.path.toString(), inputBuilder)
    httpDef.parseRequestParametersInto(uri.query().toMultiMap, inputBuilder)

    val wrappedBody = buildWrappedBody(httpDef, inputBuilder, body)

    RestDeferredCall[Any, R](
      message = wrappedBody,
      metadata = MetadataImpl.Empty,
      fullServiceName = httpDef.methodDescriptor.getService.getFullName,
      methodName = httpDef.methodDescriptor.getName,
      asyncCall = (metadata: Metadata) =>
        asyncCall(metadata).exceptionally {
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
