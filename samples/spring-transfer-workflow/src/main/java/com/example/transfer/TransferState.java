package com.example.transfer;

import static com.example.transfer.TransferState.TransferStatus.*;

// tag::domain[]
public record TransferState(Transfer transfer, TransferStatus status) {

  public record Transfer(String from, String to, int amount) { // <1>
  }

  public enum TransferStatus { // <2>
    STARTED, MANUAL_APPROVAL_REQUIRED, ACCEPTED, SUCCESSFUL_WITHDRAWAL, COMPLETED, REJECTED
  }
  // end::domain[]

  public TransferState(Transfer transfer) {
    this(transfer, STARTED);
  }

  public TransferState asAccepted() {
    return new TransferState(transfer, ACCEPTED);
  }

  public TransferState asCompleted() {
    return new TransferState(transfer, COMPLETED);
  }

  public TransferState asManualApprovalRequired() {
    return new TransferState(transfer, MANUAL_APPROVAL_REQUIRED);
  }

  public TransferState asSuccessfulWithdrawal() {
    return new TransferState(transfer, SUCCESSFUL_WITHDRAWAL);
  }

  public TransferState asRejected() {
    return new TransferState(transfer, REJECTED);
  }

  public boolean requiresApproval() {
    return status == MANUAL_APPROVAL_REQUIRED;
  }
}
