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

package kalix.javasdk.impl.workflow

import com.example.workflow.transfer.MoneyTransferApi
import com.google.protobuf.Empty
import io.grpc.Status.Code.INVALID_ARGUMENT
import kalix.javasdk.impl.AnySupport
import kalix.javasdk.impl.workflow.WorkflowImplSpec.MoneyTransfer
import kalix.javasdk.workflow.TransferWorkflow
import kalix.javasdk.workflow.TransferWorkflowProvider
import kalix.protocol.workflow_entity.WorkflowStreamIn
import kalix.testkit.TestProtocol
import org.scalatest.BeforeAndAfterAll
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.google.protobuf.any.{ Any => ScalaPbAny }

class WorkflowImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with OptionValues {
  import kalix.testkit.workflow.WorkflowMessages._

  private val service: TestWorkflow = MoneyTransfer.testWorkflow
  private val protocol: TestProtocol = TestProtocol(service.port)

  override def afterAll(): Unit = {
    protocol.terminate()
    service.terminate()

  }

  "WorkflowEntityImpl" should {
    "fail when first message is not init" in {
      service.expectLogError("Terminating workflow due to unexpected failure") {
        val workflow = protocol.workflow.connect()
        workflow.send(command(1, "transfer", "start"))
        val message = workflow.expectNext()
        val failure = message.failure.get
        failure.description should startWith("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when entity is sent multiple init" in {
      service.expectLogError("Terminating workflow [transfer] due to unexpected failure") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when service doesn't exist" in {
      service.expectLogError("Terminating workflow [foo] due to unexpected failure") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(serviceName = "DoesNotExist", workflowId = "foo"))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when command entity id is incorrect" in {
      service.expectLogError("Terminating workflow [transfer2] due to unexpected failure for command [foo]") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.send(command(1, "transfer2", "foo"))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when command payload is missing" in {
      service.expectLogError("Terminating workflow [transfer] due to unexpected failure for command [foo]") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.send(command(1, "transfer", "foo", payload = None))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when entity is sent empty message" in {
      service.expectLogError("Terminating workflow [transfer] due to unexpected failure") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.send(WorkflowStreamIn.Message.Empty)
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when command handler does not exist" in {
      service.expectLogError("Terminating workflow [transfer] due to unexpected failure for command [foo]") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.send(command(1, "transfer", "foo"))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail action when command handler returns error effect" in {
      val workflow = protocol.workflow.connect()
      workflow.send(init(MoneyTransfer.Name, "transfer"))
      workflow.send(command(1, "transfer", "Start", MoneyTransfer.transfer("transfer", "foo", "bar", -1)))
      workflow.expect(actionFailure(1, "Transfer amount cannot be negative.", INVALID_ARGUMENT))

      // TODO: check update-then-fail doesn't change workflow state
      // to properly tests it, we need to put the workflow in a waiting state
      // first we initiate it with some start command, then we wait for more input
      // the test should then send a invalid command without affecting workflow's state
    }

    "fail when command handler throws exception" in {
      service.expectLogError("Terminating workflow [transfer] due to unexpected failure for command [IllegalCall]") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.send(command(1, "transfer", "IllegalCall", MoneyTransfer.transfer("transfer", "foo", "bar", 10)))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "run workflow to completion" in {
      val workflow = protocol.workflow.connect()
      val workflowId = "transfer"
      workflow.send(init(MoneyTransfer.Name, workflowId))

      // start the workflow
      workflow.send(command(1, workflowId, "Start", MoneyTransfer.transfer(workflowId, "foo", "bar", 10)))

      val messageFromStartCmd = workflow.expectNext()
      messageFromStartCmd.isEffect shouldBe true

      val startEffect = messageFromStartCmd.effect.value
      val startState = MoneyTransfer.decode[MoneyTransferApi.State](startEffect.userState.value)
      startState.getFrom shouldBe "foo"
      startState.getTo shouldBe "bar"
      startState.getAmount shouldBe 10.0
      startState.getLastStep shouldBe "started"

      val transitionToWithdraw = startEffect.transition.stepTransition.value
      transitionToWithdraw.stepName shouldBe "withdraw"
      // cast will fail if not Withdraw type
      val withdrawInput = MoneyTransfer.decode[MoneyTransferApi.Withdraw](transitionToWithdraw.input.value)

      val startClientAction = startEffect.clientAction.value
      // cast will fail if not Empty type
      MoneyTransfer.decode[Empty](startClientAction.action.reply.value.payload.value)
      //-----------------------------------------------------------------

      // run next step, ie: Withdraw
      workflow.send(executeStep(2, transitionToWithdraw.stepName, withdrawInput, startState))
      val messageFromWithdraw = workflow.expectNext()
      messageFromWithdraw.isResponse shouldBe true
      val responseFromWithdraw = messageFromWithdraw.response.value
      val defCallFromWithdraw = responseFromWithdraw.response.deferredCall.value
      // cast will fail if not Withdraw type
      MoneyTransfer.decode[MoneyTransferApi.Withdraw](defCallFromWithdraw.payload.value)
      //-----------------------------------------------------------------

      // simulate withdraw successful, ask for the transition
      workflow.send(getNextStep(3, transitionToWithdraw.stepName, Empty.getDefaultInstance, startState))
      val effectAfterWithdraw = workflow.expectNext()
      effectAfterWithdraw.isEffect shouldBe true

      val withdrawEffect = effectAfterWithdraw.effect.value

      val stateAfterWithdraw = MoneyTransfer.decode[MoneyTransferApi.State](withdrawEffect.userState.value)
      stateAfterWithdraw.getFrom shouldBe "foo"
      stateAfterWithdraw.getTo shouldBe "bar"
      stateAfterWithdraw.getAmount shouldBe 10.0
      stateAfterWithdraw.getLastStep shouldBe "withdrawn"

      val transitionToDeposit = withdrawEffect.transition.stepTransition.value
      transitionToDeposit.stepName shouldBe "deposit"
      val depositInput = MoneyTransfer.decode[MoneyTransferApi.Deposit](transitionToDeposit.input.value)
      //-----------------------------------------------------------------

      // run next step, ie: Withdraw
      workflow.send(executeStep(2, transitionToDeposit.stepName, depositInput, stateAfterWithdraw))

      val messageFromDeposit = workflow.expectNext()
      messageFromDeposit.isResponse shouldBe true
      val responseFromDeposit = messageFromDeposit.response.value
      val defCallFromDeposit = responseFromDeposit.response.deferredCall.value
      // cast will fail if not Deposit type
      MoneyTransfer.decode[MoneyTransferApi.Deposit](defCallFromDeposit.payload.value)
      //-----------------------------------------------------------------

      // simulate deposit successful, ask for the transition
      workflow.send(getNextStep(3, transitionToDeposit.stepName, Empty.getDefaultInstance, stateAfterWithdraw))
      val effectAfterDeposit = workflow.expectNext()
      effectAfterDeposit.isEffect shouldBe true

      val depositEffect = effectAfterDeposit.effect.value

      val stateAfterDeposit = MoneyTransfer.decode[MoneyTransferApi.State](depositEffect.userState.value)
      stateAfterDeposit.getFrom shouldBe "foo"
      stateAfterDeposit.getTo shouldBe "bar"
      stateAfterDeposit.getAmount shouldBe 10.0
      stateAfterDeposit.getLastStep shouldBe "deposited"

      depositEffect.transition.isEndTransition shouldBe true
      //-----------------------------------------------------------------
    }

  }
}

object WorkflowImplSpec {

  object MoneyTransfer {

    val anySupport = new AnySupport(Array(MoneyTransferApi.getDescriptor), this.getClass.getClassLoader)
    def decode[T](any: ScalaPbAny): T = anySupport.decodeMessage(any).asInstanceOf[T]

    val Name: String = MoneyTransferApi.getDescriptor.findServiceByName("TransferWorkflowService").getFullName

    def testWorkflow: TestWorkflow =
      TestWorkflow.service(TransferWorkflowProvider.of(_ => new TransferWorkflow()));

    def transfer(workflowId: String, from: String, to: String, amount: Double) =
      MoneyTransferApi.Transfer
        .newBuilder()
        .setWorkflowId(workflowId)
        .setFrom(from)
        .setTo(to)
        .setAmount(amount)
        .build()

  }
}
