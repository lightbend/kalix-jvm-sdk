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

import akka.annotation.InternalApi
import kalix.javasdk.impl.ProxyInfoHolder
import kalix.springsdk.WebClientProvider
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

/**
 * INTERNAL API
 */
@InternalApi
private[springsdk] class WebClientProviderImpl(proxyInfoHolder: ProxyInfoHolder) extends WebClientProvider {

  override def webClientFor(host: String): WebClient = {
    val remoteAddHeader = proxyInfoHolder.remoteIdentificationHeader
    buildClient(host, 80, remoteAddHeader)
  }

  def localWebClient: WebClient = {
    val localAddHeader = proxyInfoHolder.localIdentificationHeader
    val clientOpt =
      for {
        host <- proxyInfoHolder.proxyHostname
        port <- proxyInfoHolder.proxyPort
      } yield buildClient(host, port, localAddHeader)

    clientOpt.getOrElse {
      throw new IllegalStateException(
        "Service proxy hostname and/or port are not set by proxy at discovery, too old proxy version?")
    }
  }

  private def buildClient(host: String, port: Int, identificationHeader: Option[(String, String)]) = {

    val builder =
      WebClient.builder
        .baseUrl(s"http://$host:$port")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)

    identificationHeader.foreach { case (key, value) =>
      builder.defaultHeader(key, value)
    }

    builder.build()

  }
}
