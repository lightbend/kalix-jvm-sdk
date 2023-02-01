package com.example.transfer;

public interface FraudDetectionResult {

  record TransferVerified(Transfer transfer) implements FraudDetectionResult {
  }

  record TransferRejected(Transfer transfer) implements FraudDetectionResult {
  }

  record TransferRequiresManualAcceptation(Transfer transfer) implements FraudDetectionResult {
  }
}
