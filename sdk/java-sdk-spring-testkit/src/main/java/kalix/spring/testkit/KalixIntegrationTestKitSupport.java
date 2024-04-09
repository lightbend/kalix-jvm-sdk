/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testkit;

import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.testkit.KalixTestKit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * This class provided the necessary infrastructure to run Kalix integration test for projects built
 * with the Java SDK. Users should let their test classes extends this class.
 *
 * <p>Requires Docker for starting a local instance of the Kalix Runtime.
 *
 * <p>This class wires-up a local Kalix application using the user's defined Kalix components.
 *
 * <p>Users can interact with their components using the {@link
 * org.springframework.web.reactive.function.client.WebClient} that is made available by the test
 * {@link org.springframework.context.ApplicationContext}.
 *
 * <p>On test teardown, the Kalix application and the Kalix Runtime (docker container) will be stopped.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class KalixIntegrationTestKitSupport {

  private Logger logger = LoggerFactory.getLogger(getClass());

  @Autowired
  private KalixTestKit kalixTestKit;

  @Autowired
  protected ComponentClient componentClient;

  protected Duration timeout = Duration.of(10, SECONDS);

  @AfterAll
  public void afterAll() {
    logger.info("Stopping Kalix TestKit...");
    kalixTestKit.stop();
  }
}
