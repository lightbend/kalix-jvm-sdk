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

package kalix.javasdk.testkit;

import kalix.javasdk.testkit.KalixTestKit.Settings.EventingSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static kalix.javasdk.testkit.KalixTestKit.Settings.EventingSupport.GOOGLE_PUBSUB;
import static kalix.javasdk.testkit.KalixTestKit.Settings.EventingSupport.KAFKA;

/** Docker test container of Kalix Runtime for local development and testing. */
public class KalixProxyContainer extends GenericContainer<KalixProxyContainer> {

  /** Default Testcontainers DockerImageName for the Kalix Runtime. */
  public static final DockerImageName DEFAULT_PROXY_IMAGE_NAME;

  /** Default proxy port (9000). */
  public static final int DEFAULT_PROXY_PORT = 9000;

  /** Default user function port (8080). */
  public static final int DEFAULT_USER_FUNCTION_PORT = 8080;

  /** Default local port where the Google Pub/Sub emulator is available (8085). */
  public static final int DEFAULT_GOOGLE_PUBSUB_PORT = 8085;

  public static final int DEFAULT_KAFKA_PORT = 9092;

  static {
    String customImage = System.getenv("KALIX_TESTKIT_PROXY_IMAGE");
    if (customImage == null) {
      DEFAULT_PROXY_IMAGE_NAME = DockerImageName.parse(BuildInfo.proxyImage()).withTag(BuildInfo.proxyVersion());
    } else {
      Logger logger = LoggerFactory.getLogger(KalixProxyContainer.class);
      DEFAULT_PROXY_IMAGE_NAME = DockerImageName.parse(customImage);
      logger.info("Using custom proxy image [{}]", customImage);
    }
  }

  private final int userFunctionPort;
  private final int eventingPort;
  private final EventingSupport eventingSupport;

  public KalixProxyContainer() {
    this(DEFAULT_USER_FUNCTION_PORT);
  }

  public KalixProxyContainer(final int userFunctionPort) {
    this(DEFAULT_PROXY_IMAGE_NAME, EventingSupport.TEST_BROKER, userFunctionPort, DEFAULT_GOOGLE_PUBSUB_PORT);
  }

  public KalixProxyContainer(EventingSupport eventingSupport, final int userFunctionPort, int eventingPort) {
    this(DEFAULT_PROXY_IMAGE_NAME, eventingSupport, userFunctionPort, eventingPort);
  }

  public KalixProxyContainer(
      final DockerImageName dockerImageName,
      EventingSupport eventingSupport,
      final int userFunctionPort,
      final int eventingPort) {
    super(dockerImageName);
    this.userFunctionPort = userFunctionPort;
    this.eventingPort = eventingPort;
    this.eventingSupport = eventingSupport;
    withExposedPorts(DEFAULT_PROXY_PORT);
    withEnv("USER_FUNCTION_HOST", "host.testcontainers.internal");
    withEnv("USER_FUNCTION_PORT", String.valueOf(userFunctionPort));
    withEnv("HTTP_PORT", String.valueOf(DEFAULT_PROXY_PORT));
    if ("false".equals(System.getenv("VERSION_CHECK_ON_STARTUP"))) {
      withEnv("VERSION_CHECK_ON_STARTUP", "false");
    }
    waitingFor(Wait.forLogMessage(".*gRPC proxy started.*", 1));
  }

  @Override
  public void start() {
    Testcontainers.exposeHostPorts(userFunctionPort);
    Testcontainers.exposeHostPorts(eventingPort);
    if (eventingSupport.equals(KAFKA)) {
      Testcontainers.exposeHostPorts(DEFAULT_KAFKA_PORT);
    } else if (eventingSupport.equals(GOOGLE_PUBSUB)) {
      Testcontainers.exposeHostPorts(DEFAULT_GOOGLE_PUBSUB_PORT);
    }
    super.start();
    // Debug tooling: pass the Proxy logs into the client SLF4J
    if ("true".equals(System.getenv("KALIX_TESTKIT_DEBUG"))) {
      Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LoggerFactory.getLogger("proxy-logs"));
      followOutput(logConsumer);
    }
  }

  /**
   * Get the mapped port for the Kalix Runtime container.
   *
   * @return port for the local Kalix service
   */
  public int getProxyPort() {
    return getMappedPort(DEFAULT_PROXY_PORT);
  }
}
