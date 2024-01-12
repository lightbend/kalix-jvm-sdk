package com.example.wallet;

import com.example.common.Response;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.example.cinema.DomainGenerators.randomWalletId;
import static com.example.wallet.Wallet.WalletCommand.ChargeWallet;
import static com.example.wallet.Wallet.WalletEvent.WalletCharged;
import static com.example.wallet.Wallet.WalletEvent.WalletCreated;
import static org.assertj.core.api.Assertions.assertThat;

class WalletEntityTest {


  @Test
  public void shouldCreateWallet() {
    //given
    var walletId = randomWalletId();
    var initialAmount = 100;
    EventSourcedTestKit<Wallet, Wallet.WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(WalletEntity::new);

    //when
    EventSourcedResult<Response> result = testKit.call(wallet -> wallet.create(walletId, initialAmount));

    //then
    assertThat(result.isReply()).isTrue();
    assertThat(result.getNextEventOfType(WalletCreated.class).initialAmount()).isEqualTo(BigDecimal.valueOf(initialAmount));
    assertThat(testKit.getState().id()).isEqualTo(walletId);
    assertThat(testKit.getState().balance()).isEqualTo(BigDecimal.valueOf(initialAmount));
  }

  @Test
  public void shouldChargeWallet() {
    //given
    var walletId = randomWalletId();
    var expenseId = "r1";
    var initialAmount = 100;
    EventSourcedTestKit<Wallet, Wallet.WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(WalletEntity::new);
    testKit.call(wallet -> wallet.create(walletId, initialAmount));
    var chargeWallet = new ChargeWallet(new BigDecimal(10), expenseId);

    //when
    EventSourcedResult<Response> result = testKit.call(wallet -> wallet.charge(chargeWallet));

    //then
    assertThat(result.isReply()).isTrue();
    assertThat(result.getNextEventOfType(WalletCharged.class)).isEqualTo(new WalletCharged(walletId, chargeWallet.amount(), expenseId));
    assertThat(testKit.getState().balance()).isEqualTo(new BigDecimal(90));
  }

  @Test
  public void shouldIgnoreChargeDuplicate() {
    //given
    var walletId = randomWalletId();
    var expenseId = "r1";
    var initialAmount = 100;
    EventSourcedTestKit<Wallet, Wallet.WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(WalletEntity::new);
    testKit.call(wallet -> wallet.create(walletId, initialAmount));
    var chargeWallet = new ChargeWallet(new BigDecimal(10), expenseId);
    testKit.call(wallet -> wallet.charge(chargeWallet));

    //when
    EventSourcedResult<Response> result = testKit.call(wallet -> wallet.charge(chargeWallet));

    //then
    assertThat(result.isReply()).isTrue();
    assertThat(result.didEmitEvents()).isFalse();
    assertThat(testKit.getState().balance()).isEqualTo(new BigDecimal(90));
  }

  @Test
  public void shouldRefundWallet() {
    //given
    var walletId = randomWalletId();
    var expenseId = "r1";
    var initialAmount = 100;
    EventSourcedTestKit<Wallet, Wallet.WalletEvent, WalletEntity> testKit = EventSourcedTestKit.of(WalletEntity::new);
    testKit.call(wallet -> wallet.create(walletId, initialAmount));
    var chargeWallet = new ChargeWallet(new BigDecimal(10), expenseId);
    testKit.call(wallet -> wallet.charge(chargeWallet));

    //when
    EventSourcedResult<Response> result = testKit.call(wallet -> wallet.charge(chargeWallet));

    //then
    assertThat(result.isReply()).isTrue();
    assertThat(result.didEmitEvents()).isFalse();
    assertThat(testKit.getState().balance()).isEqualTo(new BigDecimal(90));
  }
}