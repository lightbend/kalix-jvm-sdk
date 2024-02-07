package com.example.wallet.api

import com.example.wallet.domain.WalletState
import com.google.protobuf.empty.Empty
import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext
import org.slf4j.LoggerFactory

class WalletEntity(context: ValueEntityContext) extends AbstractWalletEntity {

  private val logger = LoggerFactory.getLogger(classOf[WalletEntity])

  override def emptyState: WalletState = null

  override def create(currentState: WalletState, initialBalance: InitialBalance): ValueEntity.Effect[Empty] =
    effects.updateState(WalletState(initialBalance.walletId, initialBalance.balance)).thenReply(Empty())

  override def withdraw(
      currentState: WalletState,
      withdrawRequest: WithdrawRequest): ValueEntity.Effect[WithdrawResult] = {
    val updatedWallet = currentState.copy(balance = currentState.balance - withdrawRequest.amount)
    if (updatedWallet.balance < 0) {
      effects.reply(WithdrawResult(WithdrawResult.Result.Failed(WithdrawFailed("Insufficient balance"))))
    } else {
      logger.info(
        "Withdraw walletId: [{}] amount -{} balance after {}",
        currentState.id,
        withdrawRequest.amount,
        updatedWallet.balance)
      effects.updateState(updatedWallet).thenReply(WithdrawResult(WithdrawResult.Result.Succeed(WithdrawSucceed())))
    }
  }

  override def deposit(currentState: WalletState, depositRequest: DepositRequest): ValueEntity.Effect[DepositResult] = {
    if (currentState == null) {
      effects.reply(DepositResult(DepositResult.Result.Failed(DepositFailed(s"Wallet ${context.entityId} not found"))))
    }
    val updatedWallet = currentState.copy(balance = currentState.balance + depositRequest.amount)
    logger.info(
      "Deposit walletId: [{}] amount +{} balance after {}",
      currentState.id,
      depositRequest.amount,
      updatedWallet.balance)
    effects.updateState(updatedWallet).thenReply(DepositResult(DepositResult.Result.Succeed(DepositSucceed())))
  }

  override def getWalletState(currentState: WalletState, getRequest: GetRequest): ValueEntity.Effect[WalletState] =
    effects.reply(currentState)

}
