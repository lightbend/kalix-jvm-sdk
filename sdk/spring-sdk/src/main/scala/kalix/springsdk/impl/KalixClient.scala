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

import com.google.protobuf.ExperimentalApi
import org.springframework.http.{ HttpHeaders, MediaType }
import org.springframework.web.reactive.function.client.WebClient

import java.util.concurrent.CompletionStage
import scala.concurrent.{ Future, Promise }
import scala.jdk.FutureConverters._
import scala.concurrent.ExecutionContext.Implicits.global

trait KalixClient {
  //def post[P, R](uri: String, body: P, returnType: Class[R]): DeferredCall[P, R]
  //def get[R](uri: String, resp: Class[R]): DeferredCall[_, R] // FIXME not sure what to use by default

  @ExperimentalApi
  def postCall[P, R](uri: String, body: P, returnType: Class[R]): CompletionStage[R]

  @ExperimentalApi
  def getCall[R](uri: String, resp: Class[R]): CompletionStage[R]
}

final class RestKalixClientImpl extends KalixClient {

  // at the time of creation, Proxy Discovery has not happened so we don't have this info
  private val host: Promise[String] = Promise[String]()
  private val port: Promise[Int] = Promise[Int]()

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

  def postCall[P, R](uri: String, body: P, returnType: Class[R]): CompletionStage[R] =
    webClient.flatMap {
      _.post()
        .uri(uri)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(returnType)
        .toFuture
        .asScala
    }.asJava

  def getCall[R](uri: String, resp: Class[R]): CompletionStage[R] =
    webClient
      .flatMap(
        _.get()
          .uri(uri)
          .retrieve()
          .bodyToMono(resp)
          .toFuture
          .asScala)
      .asJava

}
