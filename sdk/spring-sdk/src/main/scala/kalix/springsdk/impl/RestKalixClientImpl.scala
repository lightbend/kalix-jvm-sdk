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

package kalix.springsdk.impl

import java.util.concurrent.CompletionStage
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.jdk.CollectionConverters.CollectionHasAsScala
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
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.impl.RestDeferredCall
import kalix.springsdk.KalixClient
import kalix.springsdk.impl.http.HttpEndpointMethodDefinition
import kalix.springsdk.impl.http.HttpEndpointMethodDefinition.ANY_METHOD
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

/**
 * INTERNAL API
 */
final class RestKalixClientImpl(messageCodec: SpringSdkMessageCodec) extends KalixClient {

  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private var services: Seq[HttpEndpointMethodDefinition] = Seq.empty

  // At the time of creation, Proxy Discovery has not happened yet
  // and we need the ProxyInfo to build the WebClient, so we need a Promise[WebClient]
  private val promisedWebClient: Promise[WebClient] = Promise[WebClient]()

  def setWebClient(localWebClient: WebClient) = promisedWebClient.trySuccess(localWebClient)

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
    matchMethodOrThrow(HttpMethods.GET, uriStr) { httpDef =>
      requestToRestDefCall(
        uriStr,
        body = None,
        httpDef,
        () =>
          webClient
            .flatMap(
              _.get()
                .uri(uriStr)
                .retrieve()
                .bodyToMono(returnType)
                .toFuture
                .asScala)
            .asJava)
    }
  }

  override def post[P, R](uriStr: String, body: P, returnType: Class[R]): DeferredCall[Any, R] = {
    matchMethodOrThrow(HttpMethods.POST, uriStr) { httpDef =>
      requestToRestDefCall(
        uriStr,
        Some(body),
        httpDef,
        () =>
          webClient.flatMap {
            _.post()
              .uri(uriStr)
              .bodyValue(body)
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def post[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    matchMethodOrThrow(HttpMethods.POST, uriStr) { httpDef =>
      requestToRestDefCall(
        uriStr,
        None,
        httpDef,
        () =>
          webClient.flatMap {
            _.post()
              .uri(uriStr)
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def put[P, R](uriStr: String, body: P, returnType: Class[R]): DeferredCall[Any, R] = {
    matchMethodOrThrow(HttpMethods.PUT, uriStr) { httpDef =>
      requestToRestDefCall(
        uriStr,
        Some(body),
        httpDef,
        () =>
          webClient.flatMap {
            _.put()
              .uri(uriStr)
              .bodyValue(body)
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def put[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    matchMethodOrThrow(HttpMethods.PUT, uriStr) { httpDef =>
      requestToRestDefCall(
        uriStr,
        None,
        httpDef,
        () =>
          webClient.flatMap {
            _.put()
              .uri(uriStr)
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def patch[P, R](uriStr: String, body: P, returnType: Class[R]): DeferredCall[Any, R] = {
    matchMethodOrThrow(HttpMethods.PATCH, uriStr) { httpDef =>
      requestToRestDefCall(
        uriStr,
        Some(body),
        httpDef,
        () =>
          webClient.flatMap {
            _.patch()
              .uri(uriStr)
              .bodyValue(body)
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def patch[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    matchMethodOrThrow(HttpMethods.PATCH, uriStr) { httpDef =>
      requestToRestDefCall(
        uriStr,
        None,
        httpDef,
        () =>
          webClient.flatMap {
            _.patch()
              .uri(uriStr)
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  override def delete[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    matchMethodOrThrow(HttpMethods.DELETE, uriStr) { httpDef =>
      requestToRestDefCall(
        uriStr,
        None,
        httpDef,
        () =>
          webClient.flatMap {
            _.delete()
              .uri(uriStr)
              .retrieve()
              .bodyToMono(returnType)
              .toFuture
              .asScala
          }.asJava)
    }
  }

  private def matchMethodOrThrow[R](httpMethod: HttpMethod, uriStr: String)(
      createDefCall: => HttpEndpointMethodDefinition => RestDeferredCall[Any, R]) = {
    val uri = Uri(uriStr)
    services
      .find(d => (d.methodPattern == ANY_METHOD || httpMethod == d.methodPattern) && d.matches(uri.path))
      .map { createDefCall(_) }
      .getOrElse(throw HttpMethodNotFoundException(httpMethod, uri.path.toString()))
  }

  private def requestToRestDefCall[P, R](
      uriStr: String,
      body: Option[P],
      httpDef: HttpEndpointMethodDefinition,
      asyncCall: () => CompletionStage[R]): RestDeferredCall[Any, R] = {

    val uri = Uri(uriStr)
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
            throw DeferredCallResponseExceptionImpl(
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

case class DeferredCallResponseExceptionImpl(
    override val description: String,
    override val errorCode: ErrorCode,
    override val cause: Throwable)
    extends RuntimeException(cause)
    with DeferredCallResponseException

final case class HttpMethodNotFoundException(httpMethod: HttpMethod, uriStr: String)
    extends RuntimeException(s"No matching service for method=$httpMethod path=$uriStr")
