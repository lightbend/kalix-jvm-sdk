/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.testkit.replicatedentity

import akka.stream.scaladsl.Source
import akka.stream.testkit.TestPublisher
import akka.stream.testkit.scaladsl.TestSink
import kalix.protocol.replicated_entity._
import kalix.testkit.TestProtocol.TestProtocolContext

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
    private val out = client.handle(Source.fromPublisher(in)).runWith(TestSink[ReplicatedEntityStreamOut]())

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
