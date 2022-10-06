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
import akka.http.scaladsl.model.{ HttpMethod, HttpMethods, HttpRequest }
import com.google.api.AnnotationsProto
import com.google.api.HttpRule.PatternCase
import com.google.protobuf.Descriptors
import kalix.javasdk.impl.{ MetadataImpl, RestDeferredCallImpl }
import kalix.javasdk.DeferredCall
import kalix.springsdk.KalixClient
import kalix.springsdk.impl.http.HttpEndpointMethodDefinition.ANY_METHOD
import kalix.springsdk.impl.http.HttpEndpointMethodDefinition
import org.springframework.http.{ HttpHeaders, MediaType }
import org.springframework.web.reactive.function.client.WebClient

import scala.concurrent.{ Future, Promise }
import scala.jdk.FutureConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.CollectionHasAsScala

/**
 * INTERNAL API
 */
class RestKalixClientImpl extends KalixClient {

  // at the time of creation, Proxy Discovery has not happened so we don't have this info
  private val host: Promise[String] = Promise[String]()
  private val port: Promise[Int] = Promise[Int]()
  private var services: Seq[HttpEndpointMethodDefinition] = Seq.empty

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

  def setHost(host: String) = this.host.trySuccess(host)
  def setPort(port: Int) = this.port.trySuccess(port)

  def registerComponent(descriptor: Descriptors.ServiceDescriptor): Unit = {
    val httpMethods =
      HttpEndpointMethodDefinition.extractForService(descriptor)

    services = services ++ httpMethods
  }

  def init(): Unit = {}

  def post[P, R](uri: String, body: P, returnType: Class[R]): DeferredCall[P, R] = {
    matchMethodDescOpt(HttpMethods.POST, Path(uri))
      .map { methodDesc =>
        new RestDeferredCallImpl[P, R](
          message = body,
          metadata = MetadataImpl.Empty,
          methodDescriptor = methodDesc,
          asyncCall = () =>
            webClient.flatMap {
              _.post()
                .uri(uri)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(returnType)
                .toFuture
                .asScala
            }.asJava)
      }
      .getOrElse {
        throw new IllegalArgumentException("No matching service") // FIXME use another exception
      }
  }

  def get[R](uri: String, returnType: Class[R]): DeferredCall[Void, R] = {
    matchMethodDescOpt(HttpMethods.GET, Path(uri))
      .map { methodDesc =>
        new RestDeferredCallImpl[Void, R](
          message = null,
          metadata = MetadataImpl.Empty,
          methodDescriptor = methodDesc,
          asyncCall = () =>
            webClient
              .flatMap(
                _.get()
                  .uri(uri)
                  .retrieve()
                  .bodyToMono(returnType)
                  .toFuture
                  .asScala)
              .asJava)
      }
      .getOrElse {
        throw new IllegalArgumentException("No matching service") // FIXME use another exception
      }
  }

  private def matchMethodDescOpt(httpMethod: HttpMethod, uri: Path): Option[Descriptors.MethodDescriptor] = {
    services
      .filter(d => (d.methodPattern == ANY_METHOD || httpMethod == d.methodPattern) && d.matches(uri))
      .map(_.methodDescriptor)
      .headOption
  }
}
