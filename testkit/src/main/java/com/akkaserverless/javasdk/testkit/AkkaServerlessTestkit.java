/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.testkit;

import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.AkkaServerlessRunner;
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
 * <p>Requires Docker for starting a local instance of Akka Serverless.
 *
 * <p>Create an AkkaServerlessTestkit with an {@link AkkaServerless} service descriptor, and then
 * {@link #start} the testkit before testing the service with gRPC or HTTP clients. Call {@link
 * #stop} after tests are complete.
 */
public class AkkaServerlessTestkit {

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

  private static final Logger log = LoggerFactory.getLogger(AkkaServerlessTestkit.class);

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
  public AkkaServerlessTestkit(final AkkaServerless akkaServerless) {
    this(akkaServerless, Settings.DEFAULT);
  }

  /**
   * Create a new testkit for an AkkaServerless service descriptor with custom settings.
   *
   * @param akkaServerless AkkaServerless service descriptor
   * @param settings custom testkit settings
   */
  public AkkaServerlessTestkit(final AkkaServerless akkaServerless, final Settings settings) {
    this.akkaServerless = akkaServerless;
    this.settings = settings;
  }

  /**
   * Start this testkit with default configuration (loaded from {@code application.conf}).
   *
   * @return this AkkaServerlessTestkit
   */
  public AkkaServerlessTestkit start() {
    return start(ConfigFactory.load());
  }

  /**
   * Start this testkit with custom configuration (overrides {@code application.conf}).
   *
   * @param config custom test configuration for the AkkaServerlessRunner
   * @return this AkkaServerlessTestkit
   */
  public AkkaServerlessTestkit start(final Config config) {
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
    return this;
  }

  /**
   * Get the local port where the Akka Serverless service is available.
   *
   * @return local Akka Serverless port
   */
  public int getPort() {
    if (!started)
      throw new IllegalStateException("Need to start AkkaServerlessTestkit before accessing port");
    return proxyContainer.getProxyPort();
  }

  /**
   * Get an {@link ActorSystem} for creating Akka gRPC or Akka HTTP clients.
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
   */
  public GrpcClientSettings getGrpcClientSettings() {
    if (!started)
      throw new IllegalStateException(
          "Need to start AkkaServerlessTestkit before accessing gRPC client settings");
    return GrpcClientSettings.connectToServiceAt("127.0.0.1", getPort(), testSystem).withTls(false);
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
