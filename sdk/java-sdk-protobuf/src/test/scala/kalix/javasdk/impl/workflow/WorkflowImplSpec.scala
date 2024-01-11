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

package kalix.javasdk.impl.workflow

import com.example.workflow.transfer.MoneyTransferApi
import com.google.protobuf.Empty
import io.grpc.Status.Code.INVALID_ARGUMENT
import kalix.javasdk.impl.AnySupport
import kalix.javasdk.impl.workflow.WorkflowImplSpec.MoneyTransfer
import kalix.protocol.workflow_entity.WorkflowStreamIn
import kalix.testkit.TestProtocol
import org.scalatest.BeforeAndAfterAll
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.workflow.TransferWorkflow
import kalix.javasdk.workflow.TransferWorkflowProvider

class WorkflowImplSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with OptionValues {
  import kalix.testkit.workflow.WorkflowMessages._

  private val service: TestWorkflow = MoneyTransfer.testWorkflow
  private val protocol: TestProtocol = TestProtocol(service.port)

  override def afterAll(): Unit = {
    protocol.terminate()
    service.terminate()
  }

  private def assertState(state: Option[ScalaPbAny])(assertFunc: MoneyTransferApi.State => Unit): Unit =
    assertFunc(MoneyTransfer.decode[MoneyTransferApi.State](state.value))

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
        workflow.expect(config())
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when service doesn't exist" in {
      service.expectLogError("Terminating workflow [foo] due to unexpected failure") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(serviceName = "DoesNotExist", entityId = "foo"))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when command entity id is incorrect" in {
      service.expectLogError("Terminating workflow [transfer2] due to unexpected failure for command [foo]") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.expect(config())
        workflow.send(command(1, "transfer2", "foo"))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when command payload is missing" in {
      service.expectLogError("Terminating workflow [transfer] due to unexpected failure for command [foo]") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.expect(config())
        workflow.send(command(1, "transfer", "foo", payload = None))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when entity is sent empty message" in {
      service.expectLogError("Terminating workflow [transfer] due to unexpected failure") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.expect(config())
        workflow.send(WorkflowStreamIn.Message.Empty)
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail when command handler does not exist" in {
      service.expectLogError("Terminating workflow [transfer] due to unexpected failure for command [foo]") {
        val workflow = protocol.workflow.connect()
        workflow.send(init(MoneyTransfer.Name, "transfer"))
        workflow.expect(config())
        workflow.send(command(1, "transfer", "foo"))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "fail action when command handler returns error effect" in {
      val workflow = protocol.workflow.connect()
      workflow.send(init(MoneyTransfer.Name, "transfer"))
      workflow.expect(config())
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
        workflow.expect(config())
        workflow.send(command(1, "transfer", "IllegalCall", MoneyTransfer.transfer("transfer", "foo", "bar", 10)))
        workflow.expectFailure("Unexpected error")
        workflow.expectClosed()
      }
    }

    "run workflow to completion" in {
      val workflow = protocol.workflow.connect()
      val workflowId = "transfer"
      workflow.send(init(MoneyTransfer.Name, workflowId))
      workflow.expect(config())

      // start the workflow
      workflow.send(command(1, workflowId, "Start", MoneyTransfer.transfer(workflowId, "foo", "bar", 10)))

      val messageFromStartCmd = workflow.expectNext()
      messageFromStartCmd.isEffect shouldBe true

      val startEffect = messageFromStartCmd.effect.value

      startEffect.transition.isPause shouldBe true
      assertState(startEffect.userState) { state =>
        state.getFrom shouldBe "foo"
        state.getTo shouldBe "bar"
        state.getAmount shouldBe 10.0
        state.getLog shouldBe "started"
      }

      // first sign-off - covert waitForInput
      workflow.send(command(1, workflowId, "SignOff", MoneyTransfer.owner("Alice")))
      val effectFromFirstSignOff = workflow.expectNext().effect.value
      assertState(effectFromFirstSignOff.userState) { state =>
        state.getLog shouldBe "sign-off: Alice"
      }
      //-----------------------------------------------------------------

      // second sign-off - covert waitForInput
      workflow.send(command(1, workflowId, "SignOff", MoneyTransfer.owner("John")))
      val effectFromSecondSignOff = workflow.expectNext().effect.value
      assertState(effectFromSecondSignOff.userState) { state =>
        state.getLog shouldBe "sign-off: John"
      }

      val transitionToRemote = effectFromSecondSignOff.transition.stepTransition.value
      transitionToRemote.stepName shouldBe "remoteCall"
      val removeCallClientAction = startEffect.clientAction.value
      // cast will fail if not Empty type
      MoneyTransfer.decode[Empty](removeCallClientAction.action.reply.value.payload.value)
      //-----------------------------------------------------------------

      // run next step, ie: RemoteCall (asyncCall)
      workflow.send(
        executeStep(2, transitionToRemote.stepName, Empty.getDefaultInstance, effectFromSecondSignOff.userState.value))
      val messageFromRemoteCall = workflow.expectNext()
      messageFromRemoteCall.isResponse shouldBe true
      messageFromRemoteCall.response.value.response.isExecuted shouldBe true

      val asyncCallStep = messageFromRemoteCall.response.value.stepName
      // after successful call, ask for next transition
      workflow.send(getNextStep(3, asyncCallStep, Empty.getDefaultInstance))
      val effectAfterAsyncCall = workflow.expectNext().effect.value

      assertState(effectAfterAsyncCall.userState) { state =>
        state.getLog shouldBe "remote-call"
      }

      //-----------------------------------------------------------------
      // run next step, ie: Withdraw
      val transitionToWithdraw = effectAfterAsyncCall.transition.stepTransition.value
      val withdrawInput = MoneyTransfer.decode[MoneyTransferApi.Withdraw](transitionToWithdraw.input.value)

      workflow.send(executeStep(4, transitionToWithdraw.stepName, withdrawInput, effectAfterAsyncCall.userState.value))
      val responseFromWithdraw = workflow.expectNext().response.value
      val defCallFromWithdraw = responseFromWithdraw.response.deferredCall.value
      // cast will fail if not Withdraw type
      MoneyTransfer.decode[MoneyTransferApi.Withdraw](defCallFromWithdraw.payload.value)
      //-----------------------------------------------------------------

      // simulate withdraw successful, ask for the transition
      workflow.send(getNextStep(5, transitionToWithdraw.stepName, Empty.getDefaultInstance))
      val withdrawEffect = workflow.expectNext().effect.value
      assertState(withdrawEffect.userState) { state =>
        state.getLog shouldBe "withdrawn"
      }
      val transitionToDeposit = withdrawEffect.transition.stepTransition.value
      transitionToDeposit.stepName shouldBe "deposit"
      val depositInput = MoneyTransfer.decode[MoneyTransferApi.Deposit](transitionToDeposit.input.value)
      //-----------------------------------------------------------------

      // run next step, ie: Deposit
      workflow.send(executeStep(6, transitionToDeposit.stepName, depositInput, withdrawEffect.userState.value))
      val responseFromDeposit = workflow.expectNext().response.value
      val defCallFromDeposit = responseFromDeposit.response.deferredCall.value
      // cast will fail if not Deposit type
      MoneyTransfer.decode[MoneyTransferApi.Deposit](defCallFromDeposit.payload.value)
      //-----------------------------------------------------------------

      // simulate deposit successful, ask for the transition
      workflow.send(getNextStep(7, transitionToDeposit.stepName, Empty.getDefaultInstance))
      val depositEffect = workflow.expectNext().effect.value
      assertState(depositEffect.userState) { state =>
        state.getLog shouldBe "deposited"
      }
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

    def owner(name: String) =
      MoneyTransferApi.Owner.newBuilder().setName(name).build()

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
