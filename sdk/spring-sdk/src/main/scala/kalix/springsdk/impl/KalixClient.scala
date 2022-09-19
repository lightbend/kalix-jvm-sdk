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

import kalix.javasdk.DeferredCall
import kalix.javasdk.impl.{ DeferredCallImpl, MetadataImpl }
import org.springframework.web.reactive.function.client.WebClient

trait KalixClient {
  def post[P, R](uri: String, body: P, returnType: Class[R]): DeferredCall[P, R]

  def get[R](uri: String, resp: Class[R]): DeferredCall[_, R] // FIXME not sure what to use by default
}

class KalixClientImpl(webClient: WebClient) extends KalixClient {

  def post[P, R](uri: String, body: P, returnType: Class[R]): DeferredCall[P, R] = {

    DeferredCallImpl[P, R](
      message = body,
      metadata = MetadataImpl.Empty,
      fullServiceName = "", // FIXME
      methodName = "",
      asyncCall = () => {
        webClient
          .post()
          .uri(uri)
          .bodyValue(body)
          .retrieve()
          .bodyToMono(returnType)
          .toFuture
      })
  }

  def get[R](uri: String, resp: Class[R]): DeferredCall[_, R] = {

    DeferredCallImpl[String, R](
      message = "none",
      metadata = MetadataImpl.Empty,
      fullServiceName = "", // FIXME
      methodName = "",
      asyncCall = () => {
        webClient
          .get()
          .uri(uri)
          .retrieve()
          .bodyToMono(resp)
          .toFuture
      })
  }

}
