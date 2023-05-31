package com.example.transfer;

import static com.example.transfer.TransferState.TransferStatus.*;

// tag::domain[]
public record TransferState(Transfer transfer, TransferStatus status) {

  public record Transfer(String from, String to, int amount) { // <1>
  }

  public enum TransferStatus { // <2>
    STARTED, WITHDRAW_SUCCEED, COMPLETED
  }

  public TransferState(Transfer transfer) {
    this(transfer, STARTED);
  }

  public TransferState withStatus(TransferStatus newStatus) {
    return new TransferState(transfer, newStatus);
  }
}
// end::domain[]
