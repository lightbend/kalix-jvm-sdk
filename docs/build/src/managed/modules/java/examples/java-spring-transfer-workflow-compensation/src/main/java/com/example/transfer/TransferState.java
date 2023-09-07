package com.example.transfer;

import static com.example.transfer.TransferState.TransferStatus.*;

public record TransferState(String transferId, Transfer transfer, TransferStatus status) {

  public record Transfer(String from, String to, int amount) {
  }

  public enum TransferStatus {
    STARTED, WITHDRAW_FAILED, WITHDRAW_SUCCEED, DEPOSIT_FAILED, COMPLETED, COMPENSATION_COMPLETED, WAITING_FOR_ACCEPTATION, TRANSFER_ACCEPTATION_TIMED_OUT, REQUIRES_MANUAL_INTERVENTION
  }

  public TransferState(String transferId, Transfer transfer) {
    this(transferId, transfer, STARTED);
  }

  public TransferState withStatus(TransferStatus newStatus) {
    return new TransferState(transferId, transfer, newStatus);
  }
}
