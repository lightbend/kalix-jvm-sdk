package com.example.transfer.api

import com.example.transfer.domain.Deposit
import com.example.transfer.domain.TransferState
import com.example.transfer.domain.TransferStatus
import com.example.transfer.domain.TransferStatus.COMPLETED
import com.example.transfer.domain.TransferStatus.WITHDRAW_SUCCEED
import com.example.transfer.domain.Withdraw
import com.example.wallet.api.DepositRequest
import com.example.wallet.api.WithdrawRequest
import com.google.protobuf.empty.Empty
import io.grpc.Status.Code
import kalix.scalasdk.workflow.AbstractWorkflow
import kalix.scalasdk.workflow.AbstractWorkflow.Effect
import kalix.scalasdk.workflow.WorkflowContext

class TransferWorkflow(context: WorkflowContext) extends AbstractTransferWorkflow {
  override def emptyState: TransferState = null

  // tag::definition[]
  override def definition: AbstractWorkflow.WorkflowDef[TransferState] = {

    val withdraw = step("withdraw") // <1>
      .call { (withdraw: Withdraw) =>
        val withdrawRequest = WithdrawRequest(withdraw.from, withdraw.amount)
        components.walletEntity.withdraw(withdrawRequest)
      } // <2>
      .andThen { _ =>
        val deposit = Deposit(currentState().to, currentState().amount)
        effects
          .updateState(currentState().copy(status = WITHDRAW_SUCCEED))
          .transitionTo("deposit", deposit) // <3>
      }

    val deposit = step("deposit") // <1>
      .call { (deposit: Deposit) =>
        val depositRequest = DepositRequest(deposit.to, deposit.amount)
        components.walletEntity.deposit(depositRequest)
      } // <4>
      .andThen { _ =>
        effects
          .updateState(currentState().copy(status = COMPLETED))
          .end // <5>
      }

    workflow // <6>
      .addStep(withdraw)
      .addStep(deposit)
  }
  // end::definition[]

  // tag::start[]
  override def start(currentState: TransferState, transfer: Transfer): Effect[Empty] = {
    if (transfer.amount <= 0) {
      effects.error("transfer amount should be greater than zero") // <1>
    } else if (currentState != null) {
      effects.error("transfer already started") // <2>
    } else {
      val initialState = TransferState(transfer.from, transfer.to, transfer.amount, TransferStatus.STARTED) // <3>
      val withdrawInput = Withdraw(transfer.from, transfer.amount)
      effects
        .updateState(initialState) // <4>
        .transitionTo("withdraw", withdrawInput) // <5>
        .thenReply(Empty()) // <6>
    }
  }
  // end::start[]

  // tag::get-transfer[]
  override def getTransferState(currentState: TransferState, getRequest: GetRequest): Effect[TransferState] = {
    if (currentState == null) {
      effects.error("Transfer does not exist", Code.NOT_FOUND)
    } else {
      effects.reply(currentState) // <1>
    }
  }
  // end::get-transfer[]

}
