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

package com.akkaserverless.javasdk.testkit;

import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import akka.stream.Materializer;
import akka.stream.SystemMaterializer;
import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.AkkaServerlessRunner;
import com.akkaserverless.javasdk.impl.GrpcClients;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Testkit for running Akka Serverless services locally.
 *
 * <p>Requires Docker for starting a local instance of the Akka Serverless proxy.
 *
 * <p>Create an AkkaServerlessTestkit with an {@link AkkaServerless} service descriptor, and then
 * {@link #start} the testkit before testing the service with gRPC or HTTP clients. Call {@link
 * #stop} after tests are complete.
 */
public class AkkaServerlessTestKit {

  /** Settings for AkkaServerlessTestkit. */
  public static class Settings {
    /** Default stop timeout (10 seconds). */
    public static Duration DEFAULT_STOP_TIMEOUT = Duration.ofSeconds(10);
    /** Default settings for AkkaServerlessTestkit. */
    public static Settings DEFAULT = new Settings(DEFAULT_STOP_TIMEOUT);

    /** Timeout setting for stopping the local Akka Serverless test instance. */
    public final Duration stopTimeout;

    /**
     * Create new settings for AkkaServerlessTestkit.
     *
     * @param stopTimeout timeout to use when waiting for Akka Serverless to stop
     */
    public Settings(final Duration stopTimeout) {
      this.stopTimeout = stopTimeout;
    }

    /**
     * Set a custom stop timeout, for stopping the local Akka Serverless test instance.
     *
     * @param stopTimeout timeout to use when waiting for Akka Serverless to stop
     * @return updated Settings
     */
    public Settings withStopTimeout(final Duration stopTimeout) {
      return new Settings(stopTimeout);
    }
  }

  private static final Logger log = LoggerFactory.getLogger(AkkaServerlessTestKit.class);

  private final AkkaServerless akkaServerless;
  private final Settings settings;

  private boolean started = false;
  private AkkaServerlessProxyContainer proxyContainer;
  private AkkaServerlessRunner runner;
  private ActorSystem testSystem;

  /**
   * Create a new testkit for an AkkaServerless service descriptor.
   *
   * @param akkaServerless AkkaServerless service descriptor
   */
  public AkkaServerlessTestKit(final AkkaServerless akkaServerless) {
    this(akkaServerless, Settings.DEFAULT);
  }

  /**
   * Create a new testkit for an AkkaServerless service descriptor with custom settings.
   *
   * @param akkaServerless AkkaServerless service descriptor
   * @param settings custom testkit settings
   */
  public AkkaServerlessTestKit(final AkkaServerless akkaServerless, final Settings settings) {
    this.akkaServerless = akkaServerless;
    this.settings = settings;
  }

  /**
   * Start this testkit with default configuration (loaded from {@code application.conf}).
   *
   * @return this AkkaServerlessTestkit
   */
  public AkkaServerlessTestKit start() {
    return start(ConfigFactory.load());
  }

  /**
   * Start this testkit with custom configuration (overrides {@code application.conf}).
   *
   * @param config custom test configuration for the AkkaServerlessRunner
   * @return this AkkaServerlessTestkit
   */
  public AkkaServerlessTestKit start(final Config config) {
    if (started) throw new IllegalStateException("AkkaServerlessTestkit already started");
    int port = availableLocalPort();
    Map<String, Object> conf = new HashMap<>();
    conf.put("akkaserverless.user-function-port", port);
    // don't kill the test JVM when terminating the AkkaServerlessRunner
    conf.put("akkaserverless.system.akka.coordinated-shutdown.exit-jvm", "off");
    Config testConfig = ConfigFactory.parseMap(conf);
    runner = akkaServerless.createRunner(testConfig.withFallback(config));
    runner.run();
    testSystem = ActorSystem.create("AkkaServerlessTestkit");
    proxyContainer = new AkkaServerlessProxyContainer(port);
    proxyContainer.start();
    started = true;
    // pass on proxy and host to GrpcClients to allow for inter-component communication
    GrpcClients.get(runner.system()).setSelfServicePort(proxyContainer.getProxyPort());
    return this;
  }

  /**
   * Get the host name/IP address where the Akka Serverless service is available. This is relevant
   * in certain Continuous Integration environments.
   *
   * @return Akka Serverless host
   */
  public String getHost() {
    if (!started)
      throw new IllegalStateException(
          "Need to start AkkaServerlessTestkit before accessing the host name");
    return proxyContainer.getHost();
  }

  /**
   * Get the local port where the Akka Serverless service is available.
   *
   * @return local Akka Serverless port
   */
  public int getPort() {
    if (!started)
      throw new IllegalStateException(
          "Need to start AkkaServerlessTestkit before accessing the port");
    return proxyContainer.getProxyPort();
  }

  /**
   * Get an Akka gRPC client for the given service name. The same client instance is shared for the
   * test. The lifecycle of the client is managed by the SDK and it should not be stopped by user
   * code.
   *
   * @param <T> The "service" interface generated for the service by Akka gRPC
   * @param clientClass The class of a gRPC service generated by Akka gRPC
   */
  public <T> T getGrpcClient(Class<T> clientClass) {
    return GrpcClients.get(getActorSystem()).getGrpcClient(clientClass, getHost(), getPort());
  }

  /**
   * An Akka Stream materializer to use for running streams. Needed for example in a command handler
   * which accepts streaming elements but returns a single async reply once all streamed elements
   * has been consumed.
   */
  public Materializer getMaterializer() {
    return SystemMaterializer.get(getActorSystem()).materializer();
  }

  /**
   * Get an {@link ActorSystem} for creating Akka HTTP clients.
   *
   * @return test actor system
   */
  public ActorSystem getActorSystem() {
    if (!started)
      throw new IllegalStateException(
          "Need to start AkkaServerlessTestkit before accessing actor system");
    return testSystem;
  }

  /**
   * Get {@link GrpcClientSettings} for creating Akka gRPC clients.
   *
   * @return test gRPC client settings
   * @deprecated Use <code>getGrpcClient</code> instead.
   */
  @Deprecated(since = "0.8.1", forRemoval = true)
  public GrpcClientSettings getGrpcClientSettings() {
    if (!started)
      throw new IllegalStateException(
          "Need to start AkkaServerlessTestkit before accessing gRPC client settings");
    return GrpcClientSettings.connectToServiceAt(getHost(), getPort(), testSystem).withTls(false);
  }

  /** Stop the testkit and local Akka Serverless. */
  public void stop() {
    try {
      proxyContainer.stop();
    } catch (Exception e) {
      log.error("AkkaServerlessTestkit proxy container failed to stop", e);
    }
    try {
      testSystem.terminate();
      testSystem
          .getWhenTerminated()
          .toCompletableFuture()
          .get(settings.stopTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      log.error("AkkaServerlessTestkit ActorSystem failed to terminate", e);
    }
    try {
      runner
          .terminate()
          .toCompletableFuture()
          .get(settings.stopTimeout.toMillis(), TimeUnit.MILLISECONDS);
    } catch (Exception e) {
      log.error("AkkaServerlessTestkit AkkaServerlessRunner failed to terminate", e);
    }
    started = false;
  }

  /**
   * Get an available local port for testing.
   *
   * @return available local port
   */
  public static int availableLocalPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new RuntimeException("Couldn't get available local port", e);
    }
  }
}
