package com.example.wallet;

import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.control.Either;
import kalix.javasdk.annotations.TypeName;

import java.math.BigDecimal;
import java.util.function.Supplier;

import static com.example.wallet.Wallet.WalletCommandError.*;
import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

public record Wallet(String id, BigDecimal balance, Map<String, Expense> expenses,
                     Set<String> commandIds) {

    public Wallet(String id, BigDecimal balance) {
        this(id, balance, HashMap.empty(), HashSet.empty());
    }

    public static final String EMPTY_WALLET_ID = "";
    public static Wallet EMPTY_WALLET = new Wallet(EMPTY_WALLET_ID, BigDecimal.ZERO, HashMap.empty(), HashSet.empty());

    private boolean isDuplicate(WalletCommand command) {
        if (command instanceof WalletCommand.RequiresDeduplicationCommand c) {
            return commandIds.contains(c.commandId());
        } else {
            return false;
        }
    }

    private Either<WalletCommandError, WalletEvent> ifExists(Supplier<Either<WalletCommandError, WalletEvent>> processingResultSupplier) {
        if (isEmpty()) {
            return left(WALLET_NOT_FOUND);
        } else {
            return processingResultSupplier.get();
        }
    }

    public Either<WalletCommandError, WalletEvent> handleCreate(String walletId, WalletCommand.CreateWallet createWallet) {
        if (isDuplicate(createWallet)) {
            return Either.left(DUPLICATED_COMMAND);
        } else {
            if (isEmpty()) {
                return right(new WalletEvent.WalletCreated(walletId, createWallet.initialAmount()));
            } else {
                return left(WALLET_ALREADY_EXISTS);
            }
        }
    }

    public Either<WalletCommandError, WalletEvent> handleCharge(WalletCommand.ChargeWallet charge) {
        if (isDuplicate(charge)) {
            return Either.left(DUPLICATED_COMMAND);
        } else {
            if (balance.compareTo(charge.amount()) < 0) {
                return right(new WalletEvent.WalletChargeRejected(id, charge.expenseId()));
            } else {
                return right(new WalletEvent.WalletCharged(id, charge.amount(), charge.expenseId()));
            }
        }
    }

    public Either<WalletCommandError, WalletEvent> handleRefund(WalletCommand.Refund refund) {
        return expenses.get(refund.chargeExpenseId()).fold(
                () -> left(EXPENSE_NOT_FOUND),
                expense -> right(new WalletEvent.WalletRefunded(id, expense.amount(), expense.expenseId(), refund.commandId()))
        );
    }

    public Wallet apply(WalletEvent event) {
        return switch (event) {
            case WalletEvent.WalletCreated walletCreated ->
                    new Wallet(walletCreated.walletId(), walletCreated.initialAmount(), expenses, commandIds);
            case WalletEvent.WalletCharged charged -> {
                Expense expense = new Expense(charged.expenseId(), charged.amount());
                yield new Wallet(id, balance.subtract(charged.amount()), expenses.put(expense.expenseId(), expense), commandIds.add(charged.expenseId()));
            }
            case WalletEvent.WalletRefunded refunded ->
                    new Wallet(id, balance.add(refunded.amount()), expenses.remove(refunded.chargeExpenseId()), commandIds.add(refunded.refundExpenseId()));
            case WalletEvent.WalletChargeRejected __ -> this;
        };
    }

    public boolean isEmpty() {
        return id.equals(EMPTY_WALLET_ID);
    }

    public record Expense(String expenseId, BigDecimal amount) {}

    /**
     * API
     */

    public enum WalletCommandError {
        WALLET_ALREADY_EXISTS, WALLET_NOT_FOUND, NOT_SUFFICIENT_FUNDS, DEPOSIT_LE_ZERO, DUPLICATED_COMMAND, EXPENSE_NOT_FOUND
    }

    sealed public interface WalletCommand {

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

    public record WalletResponse(String id, BigDecimal balance) {
        public static WalletResponse from(Wallet wallet) {
            return new WalletResponse(wallet.id(), wallet.balance());
        }
    }

    /**
     * Events
     */

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

}
