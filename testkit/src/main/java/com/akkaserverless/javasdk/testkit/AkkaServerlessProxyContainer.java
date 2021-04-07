/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.testkit;

import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/** Docker test container of Akka Serverless proxy for local development and testing. */
public class AkkaServerlessProxyContainer extends GenericContainer<AkkaServerlessProxyContainer> {

  /** Default Akka Serverless proxy image for local testing. */
  public static final String DEFAULT_PROXY_IMAGE = BuildInfo.proxyImage();

  /** Default Akka Serverless proxy version, compatible with this version of the SDK and testkit. */
  public static final String DEFAULT_PROXY_VERSION = BuildInfo.proxyVersion();

  /** Default Testcontainers DockerImageName for the Akka Serverless proxy. */
  public static final DockerImageName DEFAULT_PROXY_IMAGE_NAME =
      DockerImageName.parse(DEFAULT_PROXY_IMAGE).withTag(DEFAULT_PROXY_VERSION);

  /** Default proxy port (9000). */
  public static final int DEFAULT_PROXY_PORT = 9000;

  /** Default user function port (8080). */
  public static final int DEFAULT_USER_FUNCTION_PORT = 8080;

  private final int userFunctionPort;

  public AkkaServerlessProxyContainer() {
    this(DEFAULT_USER_FUNCTION_PORT);
  }

  public AkkaServerlessProxyContainer(final int userFunctionPort) {
    this(DEFAULT_PROXY_IMAGE_NAME, userFunctionPort);
  }

  public AkkaServerlessProxyContainer(
      final DockerImageName dockerImageName, final int userFunctionPort) {
    super(dockerImageName);
    this.userFunctionPort = userFunctionPort;
    withExposedPorts(DEFAULT_PROXY_PORT);
    withEnv("USER_FUNCTION_HOST", "host.testcontainers.internal");
    withEnv("USER_FUNCTION_PORT", String.valueOf(userFunctionPort));
    withEnv("HTTP_PORT", String.valueOf(DEFAULT_PROXY_PORT));
    waitingFor(Wait.forLogMessage(".*Akka Serverless proxy online.*", 1));
  }

  @Override
  public void start() {
    Testcontainers.exposeHostPorts(userFunctionPort);
    super.start();
  }

  /**
   * Get the mapped port for the Akka Serverless proxy container.
   *
   * @return port for the local Akka Serverless service
   */
  public int getProxyPort() {
    return getMappedPort(DEFAULT_PROXY_PORT);
  }
}
