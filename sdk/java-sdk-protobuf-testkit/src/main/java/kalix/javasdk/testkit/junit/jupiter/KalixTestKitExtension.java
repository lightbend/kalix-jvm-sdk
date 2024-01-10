/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.testkit.junit.jupiter;

import akka.actor.ActorSystem;
import akka.grpc.GrpcClientSettings;
import akka.stream.Materializer;
import kalix.javasdk.Kalix;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.testkit.EventingTestKit;
import kalix.javasdk.testkit.EventingTestKit.IncomingMessages;
import kalix.javasdk.testkit.EventingTestKit.OutgoingMessages;
import kalix.javasdk.testkit.EventingTestKit.Topic;
import kalix.javasdk.testkit.KalixTestKit;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.ExternalResource;

/**
 * A JUnit 5 "Jupiter" Extension for {@link KalixTestKit}, which automatically manages the lifecycle of
 * the testkit. The testkit will be automatically stopped when the test completes or fails.
 *
 * <p>Example:
 *
 * <pre>
 * import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
 *
 * public class MyKalixIntegrationTest {
 *
 *   private static final Kalix MY_KALIX = new Kalix(); // with registered services
 *
 *   &#64;RegisterExtension
 *   public static final KalixTestKitExtension testKit = new KalixTestKitExtension(MY_KALIX);
 *
 *   private final MyServiceClient client; // generated Akka gRPC client
 *
 *   public MyKalixIntegrationTest() {
 *     this.client = MyServiceClient.create(testKit.getGrpcClientSettings(), testKit.getActorSystem());
 *   }
 *
 *   &#64;Test
 *   public void test() {
 *     // use client to test service
 *   }
 * }
 * </pre>
 */
public final class KalixTestKitExtension implements BeforeAllCallback, AfterAllCallback {

  private final KalixTestKit testKit;

  public KalixTestKitExtension(Kalix kalix) {
    this(kalix, kalix.getMessageCodec(), KalixTestKit.Settings.DEFAULT);
  }

  public KalixTestKitExtension(Kalix kalix, KalixTestKit.Settings settings) {
    this(kalix, kalix.getMessageCodec(), settings);
  }

  public KalixTestKitExtension(Kalix kalix, MessageCodec messageCodec, KalixTestKit.Settings settings) {
    this.testKit = new KalixTestKit(kalix, messageCodec, settings);
  }


  /**
   * JUnit5 support - extension based
   */
  @Override
  public void afterAll(ExtensionContext extensionContext) throws Exception {
    testKit.stop();
  }

  /**
   * JUnit5 support - extension based
   */
  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
    testKit.start();
  }


  /**
   * Get incoming messages for ValueEntity.
   *
   * @param typeId @TypeId or entity_type of the ValueEntity (depending on the used SDK)
   */
  public IncomingMessages getValueEntityIncomingMessages(String typeId) {
    return testKit.getValueEntityIncomingMessages(typeId);
  }

  /**
   * Get incoming messages for EventSourcedEntity.
   *
   * @param typeId @TypeId or entity_type of the EventSourcedEntity (depending on the used SDK)
   */
  public IncomingMessages getEventSourcedEntityIncomingMessages(String typeId) {
    return testKit.getEventSourcedEntityIncomingMessages(typeId);
  }

  /**
   * Get incoming messages for Stream (eventing.in.direct in case of protobuf SDKs).
   *
   * @param service  service name
   * @param streamId service stream id
   */
  public IncomingMessages getStreamIncomingMessages(String service, String streamId) {
    return testKit.getStreamIncomingMessages(service, streamId);
  }

  /**
   * Get incoming messages for Topic.
   *
   * @param topic topic name
   */
  public IncomingMessages getTopicIncomingMessages(String topic) {
    return testKit.getTopicIncomingMessages(topic);
  }

  /**
   * Get mocked topic destination.
   *
   * @param topic topic name
   */
  public OutgoingMessages getTopicOutgoingMessages(String topic) {
    return testKit.getTopicOutgoingMessages(topic);
  }

  /**
   * Returns {@link EventingTestKit.MessageBuilder} utility
   * to create {@link EventingTestKit.Message}s for the eventing testkit.
   */
  public EventingTestKit.MessageBuilder getMessageBuilder() {
    return testKit.getMessageBuilder();
  }

  /**
   * Get the host name/IP address where the Kalix service is available. This is relevant in certain
   * Continuous Integration environments.
   *
   * @return Kalix host
   */
  public String getHost() {
    return testKit.getHost();
  }

  /**
   * Get the local port where the Kalix service is available.
   *
   * @return local Kalix port
   */
  public int getPort() {
    return testKit.getPort();
  }

  /**
   * Get an Akka gRPC client for the given service name. The same client instance is shared for the
   * test. The lifecycle of the client is managed by the SDK and it should not be stopped by user
   * code.
   *
   * @param <T>         The "service" interface generated for the service by Akka gRPC
   * @param clientClass The class of a gRPC service generated by Akka gRPC
   */
  public <T> T getGrpcClient(Class<T> clientClass) {
    return testKit.getGrpcClient(clientClass);
  }

  /**
   * An Akka Stream materializer to use for running streams. Needed for example in a command handler
   * which accepts streaming elements but returns a single async reply once all streamed elements
   * has been consumed.
   */
  public Materializer getMaterializer() {
    return testKit.getMaterializer();
  }

  /**
   * Get an {@link ActorSystem} for creating Akka HTTP clients.
   *
   * @return test actor system
   */
  public ActorSystem getActorSystem() {
    return testKit.getActorSystem();
  }


}
