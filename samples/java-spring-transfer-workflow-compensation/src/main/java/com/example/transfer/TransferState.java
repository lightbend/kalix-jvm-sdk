package com.example.transfer;

import static com.example.transfer.TransferState.TransferStatus.*;

// tag::domain[]
public record TransferState(Transfer transfer, TransferStatus status) {

  public record Transfer(String from, String to, int amount) { // <1>
  }

  public enum TransferStatus { // <2>
    STARTED, WITHDRAW_FAILED, WITHDRAW_SUCCEED, DEPOSIT_FAILED, COMPLETED, COMPENSATION_COMPLETED, REQUIRES_MANUAL_INTERVENTION
  }

  public TransferState(Transfer transfer) {
    this(transfer, STARTED);
  }

  public TransferState withStatus(TransferStatus newStatus) {
    return new TransferState(transfer, newStatus);
  }
}
// end::domain[]
