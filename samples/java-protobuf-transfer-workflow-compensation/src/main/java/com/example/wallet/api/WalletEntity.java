package com.example.wallet.api;

import com.example.wallet.domain.WalletDomain;
import com.google.protobuf.Empty;
import kalix.javasdk.valueentity.ValueEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Value Entity Service described in your com/example/wallet/wallet_api.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class WalletEntity extends AbstractWalletEntity {
  @SuppressWarnings("unused")
  private static final Logger logger = LoggerFactory.getLogger(WalletEntity.class);

  @SuppressWarnings("unused")
  private final String entityId;

  public WalletEntity(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public WalletDomain.WalletState emptyState() {
    return null;
  }

  @Override
  public Effect<Empty> create(WalletDomain.WalletState currentState, WalletApi.InitialBalance initialBalance) {
    WalletDomain.WalletState newWallet = WalletDomain.WalletState.newBuilder().setBalance(initialBalance.getBalance()).build();
    return effects().updateState(newWallet)
      .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<WalletApi.WithdrawResult> withdraw(WalletDomain.WalletState currentState, WalletApi.WithdrawRequest withdrawRequest) {
    WalletDomain.WalletState updatedWallet = currentState.toBuilder().setBalance(currentState.getBalance() - withdrawRequest.getAmount()).build();
    if (updatedWallet.getBalance() < 0) {
      WalletApi.WithdrawResult insufficientBalance = WalletApi.WithdrawResult.newBuilder()
        .setFailed(WalletApi.WithdrawFailed.newBuilder()
          .setMessage("Insufficient balance")
          .build())
        .build();
      return effects().reply(insufficientBalance);
    } else {
      logger.info("Withdraw walletId: [{}] amount -{} balance after {}", currentState().getId(), withdrawRequest.getAmount(), updatedWallet.getBalance());
      WalletApi.WithdrawResult succeed = WalletApi.WithdrawResult.newBuilder()
        .setSucceed(WalletApi.WithdrawSucceed.getDefaultInstance())
        .build();
      return effects().updateState(updatedWallet).thenReply(succeed);
    }
  }

  @Override
  public Effect<WalletApi.DepositResult> deposit(WalletDomain.WalletState currentState, WalletApi.DepositRequest depositRequest) {
    if (currentState() == null) {
      WalletApi.DepositResult walletNotExists = WalletApi.DepositResult.newBuilder()
        .setFailed(WalletApi.DepositFailed.newBuilder()
          .setMessage("Wallet [" + commandContext().entityId() + "] not found")
          .build())
        .build();
      return effects().reply(walletNotExists);
    } else {
      WalletDomain.WalletState updatedWallet = currentState.toBuilder().setBalance(currentState.getBalance() + depositRequest.getAmount()).build();
      logger.info("Deposit walletId: [{}] amount +{} balance after {}", currentState().getId(), depositRequest.getAmount(), updatedWallet.getBalance());
      WalletApi.DepositResult succeed = WalletApi.DepositResult.newBuilder()
        .setSucceed(WalletApi.DepositSucceed.getDefaultInstance())
        .build();
      return effects().updateState(updatedWallet).thenReply(succeed);
    }
  }

  @Override
  public Effect<WalletDomain.WalletState> getWalletState(WalletDomain.WalletState currentState, WalletApi.GetRequest getRequest) {
    return effects().reply(currentState);
  }
}
