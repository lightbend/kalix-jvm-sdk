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
import kalix.javasdk.Metadata;
import kalix.javasdk.testkit.impl.EventingTestKitImpl;

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
    Message<ByteString> expectNext();

    /**
     * Waits for a default amount of time before returning all unread messages in the topic.
     * If no message is received, a timeout exception is thrown.
     *
     * @return collection of messages, each message including ByteString payload and metadata
     */
    Collection<Message<ByteString>> expectAll();
  }

  @ApiMayChange
  interface Message<P> {
    P getPayload();

    Metadata getMetadata();
  }
}

