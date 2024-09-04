package com.example.wallet.api;

import com.example.wallet.api.WalletApi.DepositRequest;
import com.example.wallet.api.WalletApi.GetRequest;
import com.example.wallet.api.WalletApi.InitialBalance;
import com.example.wallet.api.WalletApi.WithdrawRequest;
import com.example.wallet.domain.WalletDomain;
import com.example.wallet.domain.WalletDomain.WalletState;
import com.google.protobuf.Empty;
import kalix.javasdk.valueentity.ValueEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Value Entity Service described in your com/example/wallet/wallet_api.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.
// tag::wallet[]
public class WalletEntity extends AbstractWalletEntity {

  // end::wallet[]
  private static final Logger logger = LoggerFactory.getLogger(WalletEntity.class);

  @SuppressWarnings("unused")
  private final String entityId;

  public WalletEntity(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public WalletState emptyState() {
    return null;
  }

  // tag::wallet[]
  @Override
  public Effect<Empty> create(WalletState currentState, InitialBalance initialBalance) { // <1>
    WalletState newWallet = WalletState.newBuilder()
      .setBalance(initialBalance.getBalance())
      .build();
    return effects().updateState(newWallet)
      .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> withdraw(WalletState currentState, WithdrawRequest withdrawRequest) { // <2>
    WalletState updatedWallet = currentState.toBuilder()
      .setBalance(currentState.getBalance() - withdrawRequest.getAmount())
      .build();
    if (updatedWallet.getBalance() < 0) {
      return effects().error("Insufficient balance");
    } else {
      // end::wallet[]
      logger.info("Withdraw walletId: [{}] amount -{} balance after {}", currentState().getId(), withdrawRequest.getAmount(), updatedWallet.getBalance());
      // tag::wallet[]
      return effects().updateState(updatedWallet).thenReply(Empty.getDefaultInstance());
    }
  }

  @Override
  public Effect<Empty> deposit(WalletState currentState, DepositRequest depositRequest) { // <3>
    WalletState updatedWallet = currentState.toBuilder()
      .setBalance(currentState.getBalance() + depositRequest.getAmount())
      .build();
    // end::wallet[]
    logger.info("Deposit walletId: [{}] amount +{} balance after {}", currentState().getId(), depositRequest.getAmount(), updatedWallet.getBalance());
    // tag::wallet[]
    return effects().updateState(updatedWallet).thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<WalletState> getWalletState(WalletState currentState, GetRequest getRequest) { // <4>
    return effects().reply(currentState);
  }
}
// end::wallet[]
