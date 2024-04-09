/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit.impl;

import kalix.javasdk.testkit.BuildInfo;
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
public class KalixRuntimeContainer extends GenericContainer<KalixRuntimeContainer> {

  /** Default Testcontainers DockerImageName for the Kalix Runtime. */
  public static final DockerImageName DEFAULT_RUNTIME_IMAGE_NAME;

  /** Default runtime port (9000). */
  public static final int DEFAULT_RUNTIME_PORT = 9000;

  /** Default user service port (8080). */
  public static final int DEFAULT_USER_SERVICE_PORT = 8080;

  /** Default local port where the Google Pub/Sub emulator is available (8085). */
  public static final int DEFAULT_GOOGLE_PUBSUB_PORT = 8085;

  public static final int DEFAULT_KAFKA_PORT = 9092;

  static {
    String customImage = System.getenv("KALIX_TESTKIT_PROXY_IMAGE");
    if (customImage == null) {
      DEFAULT_RUNTIME_IMAGE_NAME = DockerImageName.parse(BuildInfo.runtimeImage()).withTag(BuildInfo.runtimeVersion());
    } else {
      Logger logger = LoggerFactory.getLogger(KalixRuntimeContainer.class);
      DEFAULT_RUNTIME_IMAGE_NAME = DockerImageName.parse(customImage);
      logger.info("Using custom runtime image [{}]", customImage);
    }
  }

  private final int userFunctionPort;
  private final int eventingPort;
  private final EventingSupport eventingSupport;

  public KalixRuntimeContainer() {
    this(DEFAULT_USER_SERVICE_PORT);
  }

  public KalixRuntimeContainer(final int userFunctionPort) {
    this(DEFAULT_RUNTIME_IMAGE_NAME, EventingSupport.TEST_BROKER, userFunctionPort, DEFAULT_GOOGLE_PUBSUB_PORT);
  }

  public KalixRuntimeContainer(EventingSupport eventingSupport, final int userFunctionPort, int eventingPort) {
    this(DEFAULT_RUNTIME_IMAGE_NAME, eventingSupport, userFunctionPort, eventingPort);
  }

  public KalixRuntimeContainer(
      final DockerImageName dockerImageName,
      EventingSupport eventingSupport,
      final int userServicePort,
      final int eventingPort) {
    super(dockerImageName);
    this.userFunctionPort = userServicePort;
    this.eventingPort = eventingPort;
    this.eventingSupport = eventingSupport;
    withExposedPorts(DEFAULT_RUNTIME_PORT);
    withEnv("USER_SERVICE_HOST", "host.testcontainers.internal");
    withEnv("USER_SERVICE_PORT", String.valueOf(userServicePort));
    withEnv("HTTP_PORT", String.valueOf(DEFAULT_RUNTIME_PORT));
    if ("false".equals(System.getenv("VERSION_CHECK_ON_STARTUP"))) {
      withEnv("VERSION_CHECK_ON_STARTUP", "false");
    }
    waitingFor(Wait.forLogMessage(".*(gRPC proxy|Kalix Runtime) started.*", 1));
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
      Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LoggerFactory.getLogger("runtime-logs"));
      followOutput(logConsumer);
    }
  }

  /**
   * Get the mapped port for the Kalix Runtime container.
   *
   * @return port for the local Kalix service
   */
  public int getProxyPort() {
    return getMappedPort(DEFAULT_RUNTIME_PORT);
  }
}
