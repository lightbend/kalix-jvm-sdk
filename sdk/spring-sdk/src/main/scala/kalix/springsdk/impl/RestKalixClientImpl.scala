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

import com.google.api.AnnotationsProto
import com.google.api.HttpRule.PatternCase
import com.google.protobuf.Descriptors
import kalix.javasdk.impl.{ MetadataImpl, RestDeferredCallImpl }
import kalix.javasdk.DeferredCall
import kalix.springsdk.KalixClient
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
  private var services: Map[String, Descriptors.MethodDescriptor] = Map.empty

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

    val ruleToDescriptorMap =
      for (methodDescriptor: Descriptors.MethodDescriptor <- descriptor.getMethods.asScala) yield {
        val httpRule = methodDescriptor.getOptions.getExtension(AnnotationsProto.http)
        httpRule.getPatternCase match {
          case PatternCase.GET  => httpRule.getGet -> methodDescriptor
          case PatternCase.POST => httpRule.getPost -> methodDescriptor
        }
      }

    services ++= ruleToDescriptorMap
  }

  def post[P, R](uri: String, body: P, returnType: Class[R]): DeferredCall[P, R] = {
    val methodDesc = services(uri) // FIXME matching logic needs addressing

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

  def get[R](uri: String, returnType: Class[R]): DeferredCall[Void, R] = {

    val methodDesc = services(uri)
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
}
