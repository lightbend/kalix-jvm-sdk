package com.example.wallet;

import com.example.wallet.model.Wallet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.example.wallet.DomainGenerators.randomId;
import static com.example.wallet.model.WalletApiModel.WalletCommand.ChargeWallet;
import static com.example.wallet.model.WalletApiModel.WalletCommand.CreateWallet;
import static com.example.wallet.model.WalletApiModel.WalletCommandError.DUPLICATED_COMMAND;
import static com.example.wallet.model.WalletApiModel.WalletCommandError.WALLET_ALREADY_EXISTS;
import static org.assertj.core.api.Assertions.assertThat;

class WalletTest {

  @Test
  public void shouldCreateWallet() {
    //given
    var wallet = Wallet.EMPTY_WALLET;
    var walletId = "1";
    var createWallet = new CreateWallet(BigDecimal.TEN);

    //when
    var event = wallet.handleCreate(walletId, createWallet).get();
    var updatedWallet = wallet.apply(event);

    //then
    assertThat(updatedWallet.id()).isEqualTo(walletId);
    assertThat(updatedWallet.balance()).isEqualTo(createWallet.initialAmount());
  }

  @Test
  public void shouldRejectCommandIfWalletExists() {
    //given
    var walletId = "1";
    var wallet = new Wallet(walletId, BigDecimal.TEN);
    var createWallet = new CreateWallet(BigDecimal.TEN);

    //when
    var error = wallet.handleCreate(walletId, createWallet).getLeft();

    //then
    assertThat(error).isEqualTo(WALLET_ALREADY_EXISTS);
  }

  @Test
  public void shouldChargeWallet() {
    //given
    var wallet = new Wallet("1", BigDecimal.TEN);
    var chargeWallet = new ChargeWallet(BigDecimal.valueOf(3), randomId());

    //when
    var event = wallet.handleCharge(chargeWallet).get();
    var updatedWallet = wallet.apply(event);

    //then
    assertThat(updatedWallet.balance()).isEqualTo(BigDecimal.valueOf(7));
  }

  @Test
  public void shouldRejectDuplicatedCharge() {
    //given
    var wallet = new Wallet("1", BigDecimal.TEN);
    var chargeWallet = new ChargeWallet(BigDecimal.valueOf(3), randomId());

    var event = wallet.handleCharge(chargeWallet).get();
    var updatedWallet = wallet.apply(event);

    //when
    var error = updatedWallet.handleCharge(chargeWallet).getLeft();

    //then
    assertThat(error).isEqualTo(DUPLICATED_COMMAND);
  }
}