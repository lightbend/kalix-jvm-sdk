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

import akka.actor.ActorSystem;
import akka.annotation.ApiMayChange;
import akka.annotation.InternalApi;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessageV3;
import kalix.javasdk.Metadata;
import kalix.javasdk.testkit.impl.EventingTestKitImpl;

import java.time.Duration;
import java.util.Collection;


public interface EventingTestKit {

  /**
   * INTERNAL API
   */
  @InternalApi
  static EventingTestKit start(ActorSystem system, String host, int port) {
    return EventingTestKitImpl.start(system, host, port);
  }


  Topic getTopic(String topic);

  /**
   * Testkit utility to mock broker's topic. Useful when doing integration tests for services that do eventing (in or out) to a broker's topic.
   */
  @ApiMayChange
  interface Topic {
    /**
     * Waits and returns the next unread message on this topic.
     * Note the message might have been received before this method was called.
     * If no message is received, a timeout exception is thrown.
     *
     * @return message including ByteString payload and metadata
     */
    Message<ByteString> expectOne();

    /**
     * Waits for a specific amount and returns the next unread message on this topic.
     * Note the message might have been received before this method was called.
     * If no message is received, a timeout exception is thrown.
     *
     * @param timeout amount of time to wait for a message if it was not received already
     * @return message including ByteString payload and metadata
     */
    Message<ByteString> expectOne(Duration timeout);

    /**
     * Waits and returns the next unread message on this topic and automatically parses
     * and casts it to the specified given type.
     *
     * @param instance object to be used to parse the received message bytes
     * @return a Message of type T
     * @param <T> a given domain type
     */
    <T extends GeneratedMessageV3> Message<T> expectOneClassOf(T instance);

    /**
     * Waits for a default amount of time before returning all unread messages in the topic.
     * If no message is received, a timeout exception is thrown.
     *
     * @return collection of messages, each message including ByteString payload and metadata
     */
    Collection<Message<ByteString>> expectN();

    /**
     * Waits for a given amount of unread messages to be received before returning.
     * If no message is received, a timeout exception is thrown.
     *
     * @param total number of messages to wait for before returning
     * @return collection of messages, each message including ByteString payload and metadata
     */
    Collection<Message<ByteString>> expectN(int total);

    /**
     * Waits for a given amount of unread messages to be received before returning up to a given timeout.
     * If no message is received, a timeout exception is thrown.
     *
     * @param total number of messages to wait for before returning
     * @param timeout maximum amount of time to wait for the messages
     * @return collection of messages, each message including ByteString payload and metadata
     */
    Collection<Message<ByteString>> expectN(int total, Duration timeout);
  }

  @ApiMayChange
  interface Message<P> {
    P getPayload();

    Metadata getMetadata();
  }
}

