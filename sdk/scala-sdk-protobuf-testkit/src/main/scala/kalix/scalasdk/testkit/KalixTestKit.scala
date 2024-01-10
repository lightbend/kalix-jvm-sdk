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

package kalix.scalasdk.testkit

import scala.concurrent.ExecutionContext

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.Materializer
import kalix.scalasdk.{ Kalix, Principal }
import kalix.javasdk.testkit.{ KalixTestKit => JTestKit }
import kalix.javasdk.testkit.KalixTestKit.Settings.{ EventingSupport => JEventingSupport }
import kalix.scalasdk.testkit.KalixTestKit.Settings.EventingSupport
import kalix.scalasdk.testkit.KalixTestKit.Settings.GooglePubSub
import kalix.scalasdk.testkit.KalixTestKit.Settings.Kafka
import kalix.scalasdk.testkit.KalixTestKit.Settings.TestBroker
import scala.concurrent.duration.FiniteDuration
import scala.jdk.DurationConverters._

import kalix.javasdk.impl.MessageCodec

/**
 * TestKit for running Kalix services locally.
 *
 * <p>Requires Docker for starting a local instance of the Kalix Runtime.
 *
 * <p>Create a KalixTestKit with an [[Kalix]] service descriptor, and then [[KalixTestKit.start]] the testkit before
 * testing the service with gRPC or HTTP clients. Call [[KalixTestKit.stop]] after tests are complete.
 */
object KalixTestKit {
  def apply(main: Kalix): KalixTestKit =
    new KalixTestKit(new JTestKit(main.delegate))

  def apply(main: Kalix, settings: Settings): KalixTestKit =
    new KalixTestKit(new JTestKit(main.delegate, main.getMessageCodec(), settings.jSettings))

  def apply(main: Kalix, messageCodec: MessageCodec, settings: Settings): KalixTestKit =
    new KalixTestKit(new JTestKit(main.delegate, messageCodec, settings.jSettings))

  final class Settings private[KalixTestKit] (private[KalixTestKit] val jSettings: JTestKit.Settings) {
    def stopTimeout: FiniteDuration = jSettings.stopTimeout.toScala
    def serviceName: String = jSettings.serviceName
    def aclEnabled: Boolean = jSettings.aclEnabled
    def withStopTimeout(timeout: FiniteDuration): Settings = new Settings(jSettings.withStopTimeout(timeout.toJava))
    def withServiceName(name: String): Settings = new Settings(jSettings.withServiceName(name))
    def withAclDisabled(): Settings = new Settings(jSettings.withAclDisabled())
    def withAclEnabled(): Settings = new Settings(jSettings.withAclEnabled())
    def withAdvancedViews(): Settings = new Settings(jSettings.withAdvancedViews())
    def withServicePortMapping(serviceName: String, host: String, port: Int): Settings =
      new Settings(jSettings.withServicePortMapping(serviceName, host, port))
    def withEventingSupport(eventingSupport: EventingSupport): Settings = {
      val jEventingSupport = eventingSupport match {
        case TestBroker   => JEventingSupport.TEST_BROKER
        case GooglePubSub => JEventingSupport.GOOGLE_PUBSUB
        case Kafka        => JEventingSupport.KAFKA
      }
      new Settings(jSettings.withEventingSupport(jEventingSupport))
    }
    def withValueEntityIncomingMessages(typeId: String) = new Settings(
      jSettings.withValueEntityIncomingMessages(typeId))
    def withEventSourcedEntityIncomingMessages(typeId: String) = new Settings(
      jSettings.withEventSourcedEntityIncomingMessages(typeId))
    def withStreamIncomingMessages(service: String, streamId: String) = new Settings(
      jSettings.withStreamIncomingMessages(service, streamId))
    def withTopicIncomingMessages(topic: String) = new Settings(jSettings.withTopicIncomingMessages(topic))
    def withTopicOutgoingMessages(topic: String) = new Settings(jSettings.withTopicOutgoingMessages(topic))
  }

  object Settings {

    /**
     * Defined which type of eventing support is being used.
     */
    sealed trait EventingSupport

    /**
     * This is the default type used and allows the testing eventing integrations without an external broker dependency
     * running.
     */
    object TestBroker extends EventingSupport

    /**
     * Used if you want to use an external Google PubSub (or its Emulator) on your tests.
     *
     * Note: the Google PubSub broker instance needs to be started independently.
     */
    object GooglePubSub extends EventingSupport

    /**
     * Used if you want to use an external Kafka broker on your tests.
     *
     * Note: the Kafka broker instance needs to be started independently.
     */
    object Kafka extends EventingSupport
  }

  val DefaultSettings: Settings = new Settings(JTestKit.Settings.DEFAULT)

}
class KalixTestKit private (delegate: JTestKit) {
  def start(): KalixTestKit = {
    delegate.start()
    this
  }

  /**
   * Get an Akka gRPC client for the given service name. The same client instance is shared for the test. The lifecycle
   * of the client is managed by the SDK and it should not be stopped by user code.
   *
   * @tparam T
   *   The "service" interface generated for the service by Akka gRPC
   * @param clientClass
   *   The class of a gRPC service generated by Akka gRPC
   */
  def getGrpcClient[T](clientClass: Class[T]): T =
    delegate.getGrpcClient(clientClass)

  /**
   * Get an Akka gRPC client for the given service name, authenticating using the given principal. The same client
   * instance is shared for the test. The lifecycle of the client is managed by the SDK and it should not be stopped by
   * user code.
   *
   * @tparam T
   *   The "service" interface generated for the service by Akka gRPC
   * @param clientClass
   *   The class of a gRPC service generated by Akka gRPC
   * @param principal
   *   The principal to authenticate calls to the service as.
   */
  def getGrpcClientForPrincipal[T](clientClass: Class[T], principal: Principal): T =
    delegate.getGrpcClientForPrincipal(clientClass, Principal.toJava(principal))

  /**
   * Get a Topic for mocked interactions, avoiding the need for a real broker instance.
   *
   * @param topic
   *   the name of the topic configured in your service which you want to mock
   * @return
   *   mocked topic to read/publish messages from/to
   */
  @Deprecated
  def getTopic(topic: String): Topic = Topic(delegate.getTopic(topic), delegate.getMessageCodec)

  /**
   * Get incoming messages for ValueEntity.
   *
   * @param typeId
   * @TypeId
   *   or entity_type of the ValueEntity (depending on the used SDK)
   */
  def getValueEntityIncomingMessages(typeId: String): IncomingMessages = IncomingMessages(
    delegate.getValueEntityIncomingMessages(typeId))

  /**
   * Get incoming messages for EventSourcedEntity.
   *
   * @param typeId
   * @TypeId
   *   or entity_type of the EventSourcedEntity (depending on the used SDK)
   */
  def getEventSourcedEntityIncomingMessages(typeId: String): IncomingMessages = IncomingMessages(
    delegate.getEventSourcedEntityIncomingMessages(typeId))

  /**
   * Get incoming messages for Stream (eventing.in.direct in case of protobuf SDKs).
   *
   * @param service
   *   service name
   * @param streamId
   *   service stream id
   */
  def getStreamIncomingMessages(service: String, streamId: String): IncomingMessages = IncomingMessages(
    delegate.getStreamIncomingMessages(service, streamId))

  /**
   * Get incoming messages for Topic.
   *
   * @param topic
   *   topic name
   */
  def getTopicIncomingMessages(topic: String): IncomingMessages = IncomingMessages(
    delegate.getTopicIncomingMessages(topic))

  /**
   * Get mocked topic destination.
   *
   * @param topic
   *   topic name
   */
  def getTopicOutgoingMessages(topic: String): OutgoingMessages =
    OutgoingMessages(delegate.getTopicOutgoingMessages(topic), delegate.getMessageCodec)

  /**
   * An Akka Stream materializer to use for running streams. Needed for example in a command handler which accepts
   * streaming elements but returns a single async reply once all streamed elements has been consumed.
   */
  def materializer: Materializer =
    delegate.getMaterializer()

  implicit def executionContext: ExecutionContext =
    materializer.executionContext

  /**
   * Get `GrpcClientSettings` for creating Akka gRPC clients.
   *
   * @return
   *   test gRPC client settings
   */
  @deprecated("Use `getGrpcClient` instead.", since = "0.8.1")
  def grpcClientSettings: GrpcClientSettings = delegate.getGrpcClientSettings()

  /**
   * Get an `ActorSystem` for creating Akka HTTP clients.
   */
  def system: ActorSystem = delegate.getActorSystem()

  def stop(): Unit = delegate.stop()
}
