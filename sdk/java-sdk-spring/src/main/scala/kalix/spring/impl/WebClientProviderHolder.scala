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

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

import scala.jdk.CollectionConverters.MapHasAsScala
import scala.jdk.OptionConverters.RichOptional

import akka.actor.ActorSystem
import akka.actor.ClassicActorSystemProvider
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.annotation.InternalApi
import com.typesafe.config.Config
import kalix.devtools.impl.DevModeSettings
import kalix.devtools.impl.HostAndPort
import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.ProxyInfoHolder
import kalix.spring.WebClientProvider
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeFilterFunctions
import org.springframework.web.reactive.function.client.WebClient

/**
 * INTERNAL API
 */
@InternalApi
private[kalix] object WebClientProviderHolder extends ExtensionId[WebClientProviderHolder] with ExtensionIdProvider {
  override def get(system: ActorSystem): WebClientProviderHolder = super.get(system)

  override def get(system: ClassicActorSystemProvider): WebClientProviderHolder = super.get(system)

  override def createExtension(system: ExtendedActorSystem): WebClientProviderHolder =
    new WebClientProviderHolder(system)
  override def lookup: ExtensionId[_ <: Extension] = this

}

class WebClientProviderHolder(system: ExtendedActorSystem) extends Extension {
  val webClientProvider = new WebClientProviderImpl(system)
}

/**
 * INTERNAL API
 */
@InternalApi
private[kalix] class WebClientProviderImpl(system: ExtendedActorSystem) extends WebClientProvider {

  private val proxyInfoHolder = ProxyInfoHolder(system)
  private val clients: ConcurrentMap[String, WebClient] = new ConcurrentHashMap()

  private val devModeSettings = DevModeSettings.fromConfig(system.settings.config).portMappings

  private val MaxCrossServiceResponseContentLength =
    system.settings.config.getBytes("kalix.cross-service.max-content-length").toInt

  override def webClientFor(host: String): WebClient = {

    // differently from the gRPC client, we don't need to create an extra config on the fly
    // we can read the dev-mode settings directly and use it to override the host and port
    val (mappedHost, mappedPort) =
      devModeSettings
        .get(host)
        .map(HostAndPort.extract)
        .getOrElse((host, 80))

    clients.computeIfAbsent(
      host,
      _ => {
        val remoteAddHeader = proxyInfoHolder.remoteIdentificationHeader
        buildClient(mappedHost, mappedPort, remoteAddHeader)
      })
  }

  val localWebClient: WebClient = {
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
        .codecs(configurer => {
          configurer.defaultCodecs.jackson2JsonEncoder(
            new Jackson2JsonEncoder(JsonSupport.getObjectMapper, MediaType.APPLICATION_JSON))
        })
        .filter(ExchangeFilterFunctions.limitResponseSize(MaxCrossServiceResponseContentLength))

    identificationHeader.foreach { case (key, value) =>
      builder.defaultHeader(key, value)
    }

    builder.build()

  }
}
