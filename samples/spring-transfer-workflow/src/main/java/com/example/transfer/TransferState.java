package com.example.transfer;

import static com.example.transfer.TransferStatus.*;

public record TransferState(Transfer transfer, TransferStatus status, boolean finished) {

  public TransferState(Transfer transfer) {
    this(transfer, STARTED, false);
  }

  public TransferState asAccepted() {
    return new TransferState(transfer, ACCEPTED, finished);
  }

  public TransferState asCompleted() {
    return new TransferState(transfer, COMPLETED, true);
  }

  public TransferState asManualApprovalRequired() {
    return new TransferState(transfer, MANUAL_APPROVAL_REQUIRED, finished);
  }

  public TransferState asSuccessfulWithdrawal() {
    return new TransferState(transfer, SUCCESSFUL_WITHDRAWAL, true);
  }

  public TransferState asRejected() {
    return new TransferState(transfer, REJECTED, true);
  }

  public boolean requiresApproval() {
    return status == MANUAL_APPROVAL_REQUIRED && finished == false;
  }
}
