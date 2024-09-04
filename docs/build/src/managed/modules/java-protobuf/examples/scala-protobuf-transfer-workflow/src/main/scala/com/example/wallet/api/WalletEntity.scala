package com.example.wallet.api

import com.example.wallet.api
import com.example.wallet.domain.WalletState
import com.google.protobuf.empty.Empty
import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class WalletEntity(context: ValueEntityContext) extends AbstractWalletEntity {

  private val logger = LoggerFactory.getLogger(classOf[WalletEntity])

  override def emptyState: WalletState = null

  override def create(currentState: WalletState, initialBalance: InitialBalance): ValueEntity.Effect[Empty] = {
    effects.updateState(WalletState(initialBalance.walletId, initialBalance.balance)).thenReply(Empty())
  }

  override def withdraw(currentState: WalletState, withdrawRequest: WithdrawRequest): ValueEntity.Effect[Empty] = {
    val updatedWallet = currentState.copy(balance = currentState.balance - withdrawRequest.amount)
    if (updatedWallet.balance < 0) {
      effects.error("Insufficient balance")
    } else {
      logger.info(
        "Withdraw walletId: [{}] amount -{} balance after {}",
        currentState.id,
        withdrawRequest.amount,
        updatedWallet.balance)
      effects.updateState(updatedWallet).thenReply(Empty())
    }
  }

  override def deposit(currentState: WalletState, depositRequest: DepositRequest): ValueEntity.Effect[Empty] = {
    val updatedWallet = currentState.copy(balance = currentState.balance + depositRequest.amount)
    logger.info(
      "Deposit walletId: [{}] amount +{} balance after {}",
      currentState.id,
      depositRequest.amount,
      updatedWallet.balance)
    effects.updateState(updatedWallet).thenReply(Empty())
  }

  override def getWalletState(currentState: WalletState, getRequest: GetRequest): ValueEntity.Effect[WalletState] =
    effects.reply(currentState)

}
