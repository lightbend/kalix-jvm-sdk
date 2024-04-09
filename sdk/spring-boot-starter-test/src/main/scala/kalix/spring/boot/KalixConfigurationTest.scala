/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.boot

import com.typesafe.config.Config
import kalix.javasdk.JsonSupport
import kalix.javasdk.impl.JsonMessageCodec
import kalix.javasdk.testkit.KalixTestKit
import kalix.spring.impl.KalixSpringApplication
import kalix.spring.impl.WebClientProviderHolder
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

/**
 * Auto-configuration for Kalix TestKit.
 *
 * When in the classpath, it disables the default KalixConfiguration that would normally start the Kalix Runtime.
 *
 * This configuration is also marked as `@Lazy` to avoid starting the Kalix Runtime automatically. The KalixTestKit, and
 * by consequence the Kalix Runtime, will only start if a test requires it.
 *
 * For example, a test extending [[kalix.spring.testkit.KalixIntegrationTestKitSupport]] will automatically trigger the
 * initialization of the testkit, but a regular test will not. This allows for a more flexible setup in case we want
 * bootstrap the services maually.
 */
@Lazy
@AutoConfiguration
class KalixConfigurationTest(applicationContext: ApplicationContext) extends KalixConfiguration(applicationContext) {

  private val logger = LoggerFactory.getLogger(getClass)

  /** WebClient pointing to the proxy. */
  @Bean
  def createWebClient(kalixTestKit: KalixTestKit): WebClient = WebClient.builder
    .baseUrl("http://localhost:" + kalixTestKit.getPort)
    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
    .codecs(configurer => {
      configurer.defaultCodecs.jackson2JsonEncoder(
        new Jackson2JsonEncoder(JsonSupport.getObjectMapper, MediaType.APPLICATION_JSON))
    })
    .build

  @Bean
  def testkitSettings(@Autowired(required = false) settings: KalixTestKit.Settings): Option[KalixTestKit.Settings] =
    Option(settings)

  @Bean
  def kalixTestKit(
      kalixSpringApplication: KalixSpringApplication,
      config: Config,
      settingsOpt: Option[KalixTestKit.Settings]): KalixTestKit = {

    val settings = settingsOpt.getOrElse(KalixTestKit.Settings.DEFAULT)
    val kalixTestKit = new KalixTestKit(kalixSpringApplication.kalix, new JsonMessageCodec, settings)

    logger.info(s"Starting Kalix TestKit with: $settings")
    kalixTestKit.start(config)

    val holder = WebClientProviderHolder.get(kalixTestKit.getRunner.system);
    //when ComponentClient is used in integration test, we must initiate webclient before the first request
    kalixSpringApplication.kalixClient.setWebClient(holder.webClientProvider.localWebClient)

    logger.info(s"Kalix Runtime running on port: ${kalixTestKit.getPort}")
    kalixTestKit
  }

}
