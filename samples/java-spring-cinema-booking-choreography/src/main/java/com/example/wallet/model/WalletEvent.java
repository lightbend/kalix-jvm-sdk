package com.example.wallet.model;

import kalix.javasdk.annotations.TypeName;

import java.math.BigDecimal;

sealed public interface WalletEvent {

    @TypeName("wallet-created")
    record WalletCreated(String walletId, BigDecimal initialAmount) implements WalletEvent {}

    @TypeName("wallet-charged")
    record WalletCharged(String walletId, BigDecimal amount, String expenseId) implements WalletEvent {}

    @TypeName("wallet-refunded")
    record WalletRefunded(String walletId, BigDecimal amount, String chargeExpenseId, String refundExpenseId) implements WalletEvent {}

    @TypeName("wallet-charge-rejected")
    record WalletChargeRejected(String walletId, String expenseId) implements WalletEvent {}
}
