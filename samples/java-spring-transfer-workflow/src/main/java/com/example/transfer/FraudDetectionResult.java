package com.example.transfer;

import com.example.transfer.TransferState.Transfer;

public interface FraudDetectionResult {

  record TransferVerified(Transfer transfer) implements FraudDetectionResult {
  }

  record TransferRejected(Transfer transfer) implements FraudDetectionResult {
  }

  record TransferRequiresManualAcceptation(Transfer transfer) implements FraudDetectionResult {
  }
}
