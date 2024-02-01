package com.example.wallet.api;

import com.example.wallet.domain.WalletDomain;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.valueentity.ValueEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class WalletEntityTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    WalletEntityTestKit service = WalletEntityTestKit.of(WalletEntity::new);
    // // use the testkit to execute a command
    // // of events emitted, or a final updated state:
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ValueEntityResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
    // // verify the final state after the command
    // assertEquals(expectedState, service.getState());
  }

  @Test
  @Disabled("to be implemented")
  public void createTest() {
    WalletEntityTestKit service = WalletEntityTestKit.of(WalletEntity::new);
    // InitialBalance command = InitialBalance.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.create(command);
  }


  @Test
  @Disabled("to be implemented")
  public void withdrawTest() {
    WalletEntityTestKit service = WalletEntityTestKit.of(WalletEntity::new);
    // WithdrawRequest command = WithdrawRequest.newBuilder()...build();
    // ValueEntityResult<WithdrawResult> result = service.withdraw(command);
  }


  @Test
  @Disabled("to be implemented")
  public void depositTest() {
    WalletEntityTestKit service = WalletEntityTestKit.of(WalletEntity::new);
    // DepositRequest command = DepositRequest.newBuilder()...build();
    // ValueEntityResult<DepositResult> result = service.deposit(command);
  }


  @Test
  @Disabled("to be implemented")
  public void getWalletStateTest() {
    WalletEntityTestKit service = WalletEntityTestKit.of(WalletEntity::new);
    // GetRequest command = GetRequest.newBuilder()...build();
    // ValueEntityResult<WalletState> result = service.getWalletState(command);
  }

}
