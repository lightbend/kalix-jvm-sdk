/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.testkit.junit;

import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.testkit.AkkaServerlessTestkit;
import org.junit.rules.ExternalResource;

/**
 * A JUnit external resource for {@link AkkaServerlessTestkit}, which automatically manages the
 * lifecycle of the testkit. The testkit will be automatically stopped when the test completes or
 * fails.
 *
 * <p><b>Note</b>: JUnit is not provided as a transitive dependency of the Java SDK testkit module
 * but must be added explicitly to your project.
 *
 * <p>Example:
 *
 * <pre>
 * import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
 *
 * public class MyAkkaServerlessIntegrationTest {
 *
 *   private static final AkkaServerless MY_AKKA_SERVERLESS = new AkkaServerless(); // with registered services
 *
 *   &#64;ClassRule
 *   public static final AkkaServerlessTestkitResource testkit = new AkkaServerlessTestkitResource(MY_AKKA_SERVERLESS);
 *
 *   private final MyServiceClient client; // generated Akka gRPC client
 *
 *   public MyAkkaServerlessIntegrationTest() {
 *     this.client = MyServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
 *   }
 *
 *   &#64;Test
 *   public void test() {
 *     // use client to test service
 *   }
 * }
 * </pre>
 */
public final class AkkaServerlessTestkitResource extends ExternalResource {

  private final AkkaServerlessTestkit testkit;

  public AkkaServerlessTestkitResource(AkkaServerless akkaServerless) {
    this(akkaServerless, AkkaServerlessTestkit.Settings.DEFAULT);
  }

  public AkkaServerlessTestkitResource(
      AkkaServerless akkaServerless, AkkaServerlessTestkit.Settings settings) {
    this.testkit = new AkkaServerlessTestkit(akkaServerless, settings);
  }

  @Override
  protected void before() {
    testkit.start();
  }

  /**
   * Get the host name/IP address where the Akka Serverless service is available. This is relevant
   * in certain Continuous Integration environments.
   *
   * @return Akka Serverless host
   */
  public String getHost() {
    return testkit.getHost();
  }

  /**
   * Get the local port where the Akka Serverless service is available.
   *
   * @return local Akka Serverless port
   */
  public int getPort() {
    return testkit.getPort();
  }

  /**
   * Get an {@link ActorSystem} for creating Akka gRPC or Akka HTTP clients.
   *
   * @return test actor system
   */
  public ActorSystem getActorSystem() {
    return testkit.getActorSystem();
  }

  /**
   * Get {@link GrpcClientSettings} for creating Akka gRPC clients.
   *
   * @return test gRPC client settings
   */
  public GrpcClientSettings getGrpcClientSettings() {
    return testkit.getGrpcClientSettings();
  }

  @Override
  protected void after() {
    testkit.stop();
  }
}
