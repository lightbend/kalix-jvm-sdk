package com.example.transfer.api

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

import com.example.transfer.domain.Deposit
import com.example.transfer.domain.TransferState
import com.example.transfer.domain.TransferStatus
import com.example.transfer.domain.TransferStatus.COMPENSATION_COMPLETED
import com.example.transfer.domain.TransferStatus.COMPLETED
import com.example.transfer.domain.TransferStatus.DEPOSIT_FAILED
import com.example.transfer.domain.TransferStatus.REQUIRES_MANUAL_INTERVENTION
import com.example.transfer.domain.TransferStatus.TRANSFER_ACCEPTATION_TIMED_OUT
import com.example.transfer.domain.TransferStatus.WAITING_FOR_ACCEPTATION
import com.example.transfer.domain.TransferStatus.WITHDRAW_FAILED
import com.example.transfer.domain.TransferStatus.WITHDRAW_SUCCEED
import com.example.transfer.domain.Withdraw
import com.example.wallet.api.DepositRequest
import com.example.wallet.api.DepositResult
import com.example.wallet.api.WalletEntity
import com.example.wallet.api.WithdrawRequest
import com.example.wallet.api.WithdrawResult
import com.google.protobuf.empty.Empty
import io.grpc.Status.Code
import kalix.scalasdk.workflow.AbstractWorkflow
import kalix.scalasdk.workflow.AbstractWorkflow.Effect
import kalix.scalasdk.workflow.AbstractWorkflow.RecoverStrategy.maxRetries
import kalix.scalasdk.workflow.WorkflowContext
import org.slf4j.LoggerFactory

class TransferWorkflow(context: WorkflowContext) extends AbstractTransferWorkflow {

  implicit val executionContext: ExecutionContext = ExecutionContext.global

  private val logger = LoggerFactory.getLogger(classOf[WalletEntity])

  override def emptyState: TransferState = null

  override def definition: AbstractWorkflow.WorkflowDef[TransferState] = {

    val withdraw = step("withdraw")
      .asyncCall { withdraw: Withdraw =>
        logger.info("Running withdraw step: " + withdraw)
        timers
          .cancel("acceptationTimout-" + currentState().transferId)
          .flatMap { _ =>
            val withdrawRequest = WithdrawRequest(withdraw.from, withdraw.amount)
            components.walletEntity.withdraw(withdrawRequest).execute()
          }
      }
      .andThen { withdrawResult =>
        withdrawResult.result match {
          case WithdrawResult.Result.Succeed(_) =>
            val deposit = Deposit(currentState().to, currentState().amount)
            effects
              .updateState(currentState().withStatus(WITHDRAW_SUCCEED))
              .transitionTo("deposit", deposit)
          case WithdrawResult.Result.Failed(msg) =>
            logger.warn("Withdraw failed with msg: {}", msg)
            effects
              .updateState(currentState().withStatus(WITHDRAW_FAILED))
              .end
          case WithdrawResult.Result.Empty =>
            throw new IllegalStateException(s"not supported withdraw result: $withdrawResult")
        }
      }

    // tag::compensation[]
    val deposit = step("deposit")
      .call { deposit: Deposit =>
        // end::compensation[]
        logger.info("Running deposit step: " + deposit)
        // tag::compensation[]
        val depositRequest = DepositRequest(deposit.to, deposit.amount)
        components.walletEntity.deposit(depositRequest)
      }
      .andThen { depositResult => // <1>
        depositResult.result match {
          case DepositResult.Result.Succeed(_) =>
            effects
              .updateState(currentState().withStatus(COMPLETED))
              .end // <2>
          case DepositResult.Result.Failed(msg) =>
            // end::compensation[]
            logger.warn("Deposit failed with msg: {}", msg)
            // tag::compensation[]
            effects
              .updateState(currentState().withStatus(DEPOSIT_FAILED))
              .transitionTo("compensate-withdraw"); // <3>
          case DepositResult.Result.Empty =>
            throw new IllegalStateException(s"not supported deposit result: $depositResult")
        }
      }

    val compensateWithdraw = step("compensate-withdraw") // <3>
      .call { () =>
        // end::compensation[]
        logger.info("Running withdraw compensation");
        // tag::compensation[]
        val depositRequest = DepositRequest(currentState().from, currentState().amount)
        components.walletEntity.deposit(depositRequest)
      }
      .andThen { depositResult =>
        depositResult.result match {
          case DepositResult.Result.Succeed(_) =>
            effects
              .updateState(currentState().withStatus(COMPENSATION_COMPLETED))
              .end // <4>
          case _ =>
            throw new IllegalStateException("Expecting succeed operation but received: " + depositResult); // <5>
        }
      }
    // end::compensation[]

    // tag::step-timeout[]
    val failoverHandler = step("failover-handler")
      .asyncCall { () =>
        // end::step-timeout[]
        logger.info("Running workflow failed step")
        // tag::step-timeout[]
        Future.successful("handling failure").map(_ => Empty())
      }
      .andThen { _ =>
        effects
          .updateState(currentState().withStatus(REQUIRES_MANUAL_INTERVENTION))
          .end
      }
      .timeout(1.second) // <1>
    // end::step-timeout[]

    // tag::pausing[]
    val waitForAcceptation = step("wait-for-acceptation")
      .asyncCall { () =>
        val timeoutRequest = AcceptationTimeoutRequest(currentState().transferId)
        timers
          .startSingleTimer( // <1>
            "acceptationTimout-" + currentState().transferId,
            8.hours,
            components.transferWorkflow.acceptationTimeout(timeoutRequest))
          .map(_ => Empty())
      }
      .andThen { _ => effects.pause } // <2>
    // end::pausing[]

    // tag::timeouts[]
    // tag::recover-strategy[]
    workflow
      // end::recover-strategy[]
      // end::timeouts[]
      // tag::timeouts[]
      .timeout(5.seconds) // <1>
      .defaultStepTimeout(2.seconds) // <2>
      // end::timeouts[]
      // tag::recover-strategy[]
      .failoverTo("failover-handler", maxRetries(0)) // <1>
      .defaultStepRecoverStrategy(maxRetries(1).failoverTo("failover-handler")) // <2>
      .addStep(withdraw)
      .addStep(deposit, maxRetries(2).failoverTo("compensate-withdraw")) // <3>
      // end::recover-strategy[]
      .addStep(compensateWithdraw)
      .addStep(waitForAcceptation)
      .addStep(failoverHandler);
  }

  override def start(currentState: TransferState, transfer: Transfer): Effect[Empty] = {
    if (currentState != null) {
      effects.error("transfer already started") // <2>
    } else if (transfer.amount <= 0) {
      effects.error("transfer amount should be greater than zero") // <1>
    } else if (transfer.amount > 1000) {
      logger.info("Waiting for acceptation: {}", transfer)
      val initialState =
        TransferState(transfer.transferId, transfer.from, transfer.to, transfer.amount, WAITING_FOR_ACCEPTATION)
      effects
        .updateState(initialState)
        .transitionTo("wait-for-acceptation")
        .thenReply(Empty())
    } else {
      logger.info("Running transfer: {}", transfer)
      val initialState =
        TransferState(transfer.transferId, transfer.from, transfer.to, transfer.amount, TransferStatus.STARTED)
      val withdrawInput = Withdraw(transfer.from, transfer.amount)
      effects
        .updateState(initialState)
        .transitionTo("withdraw", withdrawInput)
        .thenReply(Empty())
    }
  }

  override def getTransferState(currentState: TransferState, getRequest: GetRequest): Effect[TransferState] = {
    if (currentState == null) {
      effects.error("Transfer does not exist", Code.NOT_FOUND)
    } else {
      effects.reply(currentState)
    }
  }

  // tag::resuming[]
  override def accept(currentState: TransferState, acceptRequest: AcceptRequest): AbstractWorkflow.Effect[Empty] = {
    if (currentState == null) {
      effects.error("Transfer not started")
    } else if (currentState.status == WAITING_FOR_ACCEPTATION) { // <1>
      // end::resuming[]
      logger.info("Running transfer: {}", currentState)
      // tag::resuming[]
      val withdrawInput = Withdraw(currentState.from, currentState.amount)
      effects
        .transitionTo("withdraw", withdrawInput)
        .thenReply(Empty())
    } else { // <2>
      effects.error(s"Cannot accept transfer with status: ${currentState.status}")
    }
  }
  // end::resuming[]

  override def acceptationTimeout(
      currentState: TransferState,
      acceptationTimeoutRequest: AcceptationTimeoutRequest): AbstractWorkflow.Effect[Empty] = {
    if (currentState == null) {
      effects.error("Transfer not started")
    } else if (currentState.status == WAITING_FOR_ACCEPTATION) {
      effects
        .updateState(currentState.withStatus(TRANSFER_ACCEPTATION_TIMED_OUT))
        .end
        .thenReply(Empty())
    } else {
      logger.info("Ignoring acceptation timeout for status: " + currentState.status)
      effects.reply(Empty())
    }
  }

}
