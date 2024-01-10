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

package kalix.testkit.replicatedentity

import akka.stream.scaladsl.Source
import akka.stream.testkit.TestPublisher
import akka.stream.testkit.scaladsl.TestSink
import kalix.protocol.replicated_entity._
import kalix.testkit.TestProtocol.TestProtocolContext

import scala.collection.immutable.Seq

final class TestReplicatedEntityProtocol(context: TestProtocolContext) {
  private val client = ReplicatedEntitiesClient(context.clientSettings)(context.system)

  def connect(): TestReplicatedEntityProtocol.Connection =
    new TestReplicatedEntityProtocol.Connection(client, context)

  def terminate(): Unit = client.close()
}

object TestReplicatedEntityProtocol {
  final class Connection(client: ReplicatedEntitiesClient, context: TestProtocolContext) {

    import context.system

    private val in = TestPublisher.probe[ReplicatedEntityStreamIn]()
    private val out = client.handle(Source.fromPublisher(in)).runWith(TestSink.probe[ReplicatedEntityStreamOut])

    out.ensureSubscription()

    def send(message: ReplicatedEntityStreamIn.Message): Connection = {
      in.sendNext(ReplicatedEntityStreamIn(message))
      this
    }

    def expect(message: ReplicatedEntityStreamOut.Message): Connection = {
      out.request(1).expectNext(ReplicatedEntityStreamOut(message))
      this
    }

    def expectNext(): ReplicatedEntityStreamOut.Message = {
      out.request(1).expectNext().message
    }

    def expectClosed(): Unit = {
      out.expectComplete()
      in.expectCancellation()
    }

    def expectEntityFailure(descStartingWith: String): Connection = {
      expectNext() match {
        case m: ReplicatedEntityStreamOut.Message =>
          if (m.failure.exists(_.description.startsWith(descStartingWith))) this
          else
            throw new RuntimeException(s"Expected failure starting with [$descStartingWith] but got $m")
      }
    }

    def passivate(): Unit = close()

    def close(): Unit = {
      in.sendComplete()
      out.expectComplete()
    }
  }
}
