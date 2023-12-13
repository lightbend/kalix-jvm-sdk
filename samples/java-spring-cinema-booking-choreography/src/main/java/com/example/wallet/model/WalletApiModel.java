package com.example.wallet.model;

import java.math.BigDecimal;

public interface WalletApiModel {
    sealed interface WalletCommand {

        sealed interface RequiresDeduplicationCommand extends WalletCommand {
            String commandId();
        }

        record CreateWallet(BigDecimal initialAmount) implements WalletCommand {
        }

        record ChargeWallet(BigDecimal amount, String expenseId) implements RequiresDeduplicationCommand {
            @Override
            public String commandId() {
                return expenseId;
            }
        }

        record Refund(String chargeExpenseId, String refundExpenseId) implements RequiresDeduplicationCommand {
            @Override
            public String commandId() {
                return refundExpenseId;
            }
        }

    }

    enum WalletCommandError {
        WALLET_ALREADY_EXISTS, WALLET_NOT_FOUND, NOT_SUFFICIENT_FUNDS, DEPOSIT_LE_ZERO, DUPLICATED_COMMAND, EXPENSE_NOT_FOUND
    }

    record WalletResponse(String id, BigDecimal balance) {
      public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.id(), wallet.balance());
      }
    }
}
