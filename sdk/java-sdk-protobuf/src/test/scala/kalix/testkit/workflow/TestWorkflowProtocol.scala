/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.testkit.workflow

import akka.stream.scaladsl.Source
import akka.stream.testkit.TestPublisher
import akka.stream.testkit.scaladsl.TestSink
import kalix.protocol.workflow_entity.WorkflowEntitiesClient
import kalix.protocol.workflow_entity.WorkflowStreamIn
import kalix.protocol.workflow_entity.WorkflowStreamOut
import kalix.testkit.TestProtocol.TestProtocolContext

final class TestWorkflowProtocol(context: TestProtocolContext) {

  private val client = WorkflowEntitiesClient(context.clientSettings)(context.system)

  def connect(): TestWorkflowProtocol.Connection =
    new TestWorkflowProtocol.Connection(client, context)

  def terminate(): Unit = client.close()

}

object TestWorkflowProtocol {

  final class Connection(client: WorkflowEntitiesClient, context: TestProtocolContext) {

    import context.system

    private val in = TestPublisher.probe[WorkflowStreamIn]()
    private val out = client.handle(Source.fromPublisher(in)).runWith(TestSink[WorkflowStreamOut]())

    out.ensureSubscription()

    def send(message: WorkflowStreamIn.Message): Connection = {
      in.sendNext(WorkflowStreamIn(message))
      this
    }

    def expect(message: WorkflowStreamOut.Message): Connection = {
      out.request(1).expectNext(WorkflowStreamOut(message))
      this
    }

    def expectNext(): WorkflowStreamOut.Message = {
      out.request(1).expectNext().message
    }

    def expectFailure(descStartingWith: String): Connection =
      expectNext() match {
        case WorkflowStreamOut.Message.Failure(failure) if failure.description.startsWith(descStartingWith) =>
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
