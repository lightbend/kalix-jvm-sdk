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

package kalix.springsdk.testkit;

import kalix.javasdk.JsonSupport;
import kalix.javasdk.testkit.KalixTestKit;
import kalix.springboot.KalixConfiguration;
import kalix.springsdk.impl.KalixSpringApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.WebClient;

/** Spring test configuration for Kalix integration tests. */
@Import(KalixConfiguration.class)
@TestConfiguration
public class KalixConfigurationTest {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired private ApplicationContext applicationContext;
  @Autowired private KalixConfiguration kalixConfiguration;

  @Bean
  public KalixSpringApplication kalixSpringApplication() {
    return new KalixSpringApplication(applicationContext, kalixConfiguration.config());
  }

  /** WebClient pointing to the proxy. */
  @Bean
  public WebClient createWebClient(KalixTestKit kalixTestKit) {
    return WebClient.builder()
        .baseUrl("http://localhost:" + kalixTestKit.getPort())
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .codecs(configurer -> {
          configurer.defaultCodecs().jackson2JsonEncoder(
            new Jackson2JsonEncoder(JsonSupport.getObjectMapper(), MediaType.APPLICATION_JSON));
        })
        .build();
  }

  @Bean
  public KalixTestKit kalixTestKit(KalixTestKit.Settings settings) {
    logger.info("Starting Kalix TestKit...");
    KalixTestKit kalixTestKit = new KalixTestKit(kalixSpringApplication().kalix(), settings);
    kalixTestKit.start(kalixConfiguration.config());
    logger.info("Kalix Proxy running on port: " + kalixTestKit.getPort());
    return kalixTestKit;
  }

  @Bean
  public KalixTestKit.Settings settings() {
    return KalixTestKit.Settings.DEFAULT;
  }
}
