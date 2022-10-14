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

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{ HttpMethod, HttpMethods, Uri }
import com.google.protobuf.{ Descriptors, DynamicMessage }
import com.google.protobuf.any.Any
import kalix.javasdk.DeferredCall
import kalix.javasdk.impl.{ AnySupport, MetadataImpl, RestDeferredCallImpl }
import kalix.protocol.component.MetadataEntry
import kalix.protocol.discovery.IdentificationInfo
import kalix.springsdk.KalixClient
import kalix.springsdk.impl.http.HttpEndpointMethodDefinition
import kalix.springsdk.impl.http.HttpEndpointMethodDefinition.ANY_METHOD
import org.slf4j.{ Logger, LoggerFactory }
import org.springframework.http.{ HttpHeaders, MediaType }
import org.springframework.web.reactive.function.client.WebClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Future, Promise }
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.FutureConverters._
import scala.util.Success

/**
 * INTERNAL API
 */
class RestKalixClientImpl(messageCodec: SpringSdkMessageCodec) extends KalixClient {
  private val logger: Logger = LoggerFactory.getLogger(getClass)

  private var services: Seq[HttpEndpointMethodDefinition] = Seq.empty

  // at the time of creation, Proxy Discovery has not happened so we don't have this info
  private val host: Promise[String] = Promise[String]()
  private val port: Promise[Int] = Promise[Int]()
  private var identificationInfo: Promise[IdentificationInfo] = Promise[IdentificationInfo]()

  private val webClient: Future[WebClient] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val baseUrl = for {
      h <- host.future
      p <- port.future
    } yield s"http://$h:$p"

    baseUrl
      .map(url => {
        WebClient.builder
          .baseUrl(url)
          .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
          .build
      })
  }

  private def buildMetadata(): MetadataImpl = {
    val entries = this.identificationInfo.future.value match {
      case Some(Success(idInfo)) =>
        remoteAddHeader(idInfo).map { case (header, token) =>
          MetadataEntry(header, MetadataEntry.Value.StringValue(token))
        }.toSeq
      case errValue =>
        logger.warn(s"Identification info not completed or failed to complete: $errValue")
        Seq.empty[MetadataEntry]
    }
    new MetadataImpl(entries)
  }

  // FIXME I don't think we need to distinguish between local and remote calls in the spring sdk.. do we?
  private def remoteAddHeader(idInfo: IdentificationInfo): Option[(String, String)] = idInfo match {
    case IdentificationInfo(_, _, header, name, _) if header.nonEmpty && name.nonEmpty =>
      Some((header, name))
    case _ => None
  }

  def setHost(host: String): Boolean = this.host.trySuccess(host)
  def setPort(port: Int): Boolean = this.port.trySuccess(port)
  def setIdentificationInfo(identificationInfo: IdentificationInfo): Unit =
    this.identificationInfo.trySuccess(identificationInfo)

  def registerComponent(descriptor: Descriptors.ServiceDescriptor): Unit = {
    services ++= HttpEndpointMethodDefinition.extractForService(descriptor)
  }

  def buildWrappedBody[P](
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

  def post[P, R](uriStr: String, body: P, returnType: Class[R]): DeferredCall[Any, R] = {
    val uri = Uri(uriStr)
    matchMethodOpt(HttpMethods.POST, uri.path)
      .map { httpDef =>
        val inputBuilder = DynamicMessage.newBuilder(httpDef.methodDescriptor.getInputType)
        httpDef.parsePathParametersInto(uri.path, inputBuilder)
        httpDef.parseRequestParametersInto(uri.query().toMultiMap, inputBuilder)
        val wrappedBody = buildWrappedBody(httpDef, inputBuilder, Some(body))

        RestDeferredCallImpl[Any, R](
          message = wrappedBody,
          metadata = buildMetadata(),
          fullServiceName = httpDef.methodDescriptor.getService.getFullName,
          methodName = httpDef.methodDescriptor.getName,
          asyncCall = () =>
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
      .getOrElse {
        throw new IllegalArgumentException(
          s"No matching service for method=${HttpMethods.GET} path=$uri"
        ) // FIXME use another exception?
      }
  }

  def get[R](uriStr: String, returnType: Class[R]): DeferredCall[Any, R] = {
    val uri = Uri(uriStr)
    matchMethodOpt(HttpMethods.GET, uri.path)
      .map { httpDef =>
        val inputBuilder = DynamicMessage.newBuilder(httpDef.methodDescriptor.getInputType)
        httpDef.parsePathParametersInto(uri.path, inputBuilder)
        httpDef.parseRequestParametersInto(uri.query().toMultiMap, inputBuilder)
        val wrappedBody = buildWrappedBody(httpDef, inputBuilder)

        RestDeferredCallImpl[Any, R](
          message = wrappedBody,
          metadata = buildMetadata(),
          fullServiceName = httpDef.methodDescriptor.getService.getFullName,
          methodName = httpDef.methodDescriptor.getName,
          asyncCall = () =>
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
      .getOrElse {
        throw new IllegalArgumentException(
          s"No matching service for method=${HttpMethods.GET} path=$uri"
        ) // FIXME use another exception?
      }
  }

  private def matchMethodOpt(httpMethod: HttpMethod, uri: Path): Option[HttpEndpointMethodDefinition] =
    services.find(d => (d.methodPattern == ANY_METHOD || httpMethod == d.methodPattern) && d.matches(uri))
}
