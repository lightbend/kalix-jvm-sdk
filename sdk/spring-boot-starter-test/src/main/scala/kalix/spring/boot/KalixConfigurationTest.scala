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

package kalix.spring.boot

import com.typesafe.config.Config
import kalix.javasdk.JsonSupport
import kalix.javasdk.testkit.KalixTestKit
import kalix.spring.impl.KalixSpringApplication
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Lazy
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.WebClient

@AutoConfiguration
class KalixConfigurationTest(applicationContext: ApplicationContext) extends KalixConfiguration(applicationContext) {

  private val logger = LoggerFactory.getLogger(getClass)

  /** WebClient pointing to the proxy. */
  @Bean
  @Lazy
  def createWebClient(kalixTestKit: KalixTestKit): WebClient = WebClient.builder
    .baseUrl("http://localhost:" + kalixTestKit.getPort)
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .codecs(configurer => {
      configurer.defaultCodecs.jackson2JsonEncoder(
        new Jackson2JsonEncoder(JsonSupport.getObjectMapper, MediaType.APPLICATION_JSON))
    })
    .build

  @Bean
  @Lazy
  def testkitSettings(@Autowired(required = false) settings: KalixTestKit.Settings): Option[KalixTestKit.Settings] =
    Option(settings)

  @Bean
  @Lazy
  def kalixTestKit(
      kalixSpringApplication: KalixSpringApplication,
      config: Config,
      settingsOpt: Option[KalixTestKit.Settings]): KalixTestKit = {

    val settings = settingsOpt.getOrElse(KalixTestKit.Settings.DEFAULT)
    val kalixTestKit = new KalixTestKit(kalixSpringApplication.kalix, settings)

    logger.info(s"Starting Kalix TestKit with: $settings")
    kalixTestKit.start(config)

    logger.info(s"Kalix Proxy running on port: ${kalixTestKit.getPort}")
    kalixTestKit
  }

}
