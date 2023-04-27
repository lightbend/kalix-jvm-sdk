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


// FIXME add docs
public interface EventingTestKit {

  static EventingTestKit start(ActorSystem system, String host, int port) {
    return EventingTestKitImpl.start(system, host, port);
  }

  String getHost();

  Integer getPort();

  Topic getTopic(String topic);

  @ApiMayChange
  interface Topic {
    Message<ByteString> expectNext();

    Collection<Message<ByteString>> expectAll();
  }

  @ApiMayChange
  interface Message<P> {
    P getPayload();

    Metadata getMetadata();
  }
}

