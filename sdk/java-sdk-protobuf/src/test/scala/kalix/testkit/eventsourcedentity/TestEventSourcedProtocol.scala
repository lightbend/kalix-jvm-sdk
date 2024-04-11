/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.testkit.eventsourcedentity

import akka.stream.scaladsl.Source
import akka.stream.testkit.TestPublisher
import akka.stream.testkit.scaladsl.TestSink
import kalix.protocol.event_sourced_entity._
import kalix.testkit.TestProtocol.TestProtocolContext

final class TestEventSourcedProtocol(context: TestProtocolContext) {
  private val client = EventSourcedEntitiesClient(context.clientSettings)(context.system)

  def connect(): TestEventSourcedProtocol.Connection = new TestEventSourcedProtocol.Connection(client, context)

  def terminate(): Unit = client.close()
}

object TestEventSourcedProtocol {
  final class Connection(client: EventSourcedEntitiesClient, context: TestProtocolContext) {

    import context.system

    private val in = TestPublisher.probe[EventSourcedStreamIn]()
    private val out = client.handle(Source.fromPublisher(in)).runWith(TestSink[EventSourcedStreamOut]())

    out.ensureSubscription()

    def send(message: EventSourcedStreamIn.Message): Connection = {
      in.sendNext(EventSourcedStreamIn(message))
      this
    }

    def expect(message: EventSourcedStreamOut.Message): Connection = {
      out.request(1).expectNext(EventSourcedStreamOut(message))
      this
    }

    def expectMessage(): EventSourcedStreamOut.Message =
      out.request(1).expectNext().message

    def expectFailure(descStartingWith: String): Connection =
      expectMessage() match {
        case EventSourcedStreamOut.Message.Failure(failure) if failure.description.startsWith(descStartingWith) =>
          this
        case other => throw new RuntimeException(s"Expected failure starting with [$descStartingWith] but got [$other]")
      }

    def expectClosed(): Unit = {
      out.expectComplete()
      in.expectCancellation()
    }

    def passivate(): Unit = close()

    def close(): Unit = {
      in.sendComplete()
      out.expectComplete()
    }
  }
}
