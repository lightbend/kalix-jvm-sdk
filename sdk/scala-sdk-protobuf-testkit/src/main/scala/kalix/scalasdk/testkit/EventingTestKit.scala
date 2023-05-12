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

package kalix.scalasdk.testkit

import akka.annotation.{ ApiMayChange, InternalApi }
import com.google.protobuf.ByteString
import kalix.javasdk.testkit.{ EventingTestKit => JEventingTestKit }
import kalix.scalasdk.Metadata
import kalix.scalasdk.testkit.impl.TopicImpl
import scalapb.GeneratedMessage
import scalapb.GeneratedMessageCompanion

import scala.concurrent.duration.FiniteDuration

/**
 * Testkit utility to mock broker's topic. Useful when doing integration tests for services that do eventing (in or out)
 * to a broker's topic.
 */
@ApiMayChange
trait Topic {

  /**
   * Waits and returns the next unread message on this topic. Note the message might have been received before this
   * method was called. If no message is received, a timeout exception is thrown.
   *
   * @return
   *   message including ByteString payload and metadata
   */
  def expectOne(): Message[ByteString]

  /**
   * Waits for a specific amount and returns the next unread message on this topic. Note the message might have been
   * received before this method was called. If no message is received, a timeout exception is thrown.
   *
   * @param timeout
   *   amount of time to wait for a message if it was not received already
   * @return
   *   message including ByteString payload and metadata
   */
  def expectOne(timeout: FiniteDuration): Message[ByteString]

  /**
   * Waits and returns the next unread message on this topic and automatically parses and casts it to the specified
   * given type.
   *
   * @param companion
   *   object to be used to parse the received message bytes
   * @tparam T
   *   a given domain type
   * @return
   *   a Message of type T
   */
  def expectOneClassOf[T <: GeneratedMessage](companion: GeneratedMessageCompanion[T]): Message[T]

  /**
   * Waits for a default amount of time before returning all unread messages in the topic. If no message is received, a
   * timeout exception is thrown.
   *
   * @return
   *   collection of messages, each message including ByteString payload and metadata
   */
  def expectN(): Seq[Message[ByteString]]

  /**
   * Waits for a given amount of unread messages to be received before returning. If no message is received, a timeout
   * exception is thrown.
   *
   * @param total
   *   number of messages to wait for before returning
   * @return
   *   collection of messages, each message including ByteString payload and metadata
   */
  def expectN(total: Int): Seq[Message[ByteString]]

  /**
   * Waits for a given amount of unread messages to be received before returning up to a given timeout. If no message is
   * received, a timeout exception is thrown.
   *
   * @param total
   *   number of messages to wait for before returning
   * @param timeout
   *   maximum amount of time to wait for the messages
   * @return
   *   collection of messages, each message including ByteString payload and metadata
   */
  def expectN(total: Int, timeout: FiniteDuration): Seq[Message[ByteString]]
}

@InternalApi
private[testkit] object Topic {
  def apply(delegate: JEventingTestKit.Topic): Topic = TopicImpl(delegate)
}

@ApiMayChange
case class Message[P](payload: P, metadata: Metadata)
