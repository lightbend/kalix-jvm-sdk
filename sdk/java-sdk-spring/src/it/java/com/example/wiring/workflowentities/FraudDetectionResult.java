/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface FraudDetectionResult {

  class TransferVerified implements FraudDetectionResult{
    public final Transfer transfer;

    @JsonCreator
    public TransferVerified(@JsonProperty("transfer") Transfer transfer) {
      this.transfer = transfer;
    }
  }

  class TransferRejected implements  FraudDetectionResult {
    public final Transfer transfer;

    @JsonCreator
    public TransferRejected(@JsonProperty("transfer") Transfer transfer) {
      this.transfer = transfer;
    }
  }

  class TransferRequiresManualAcceptation implements  FraudDetectionResult {
    public final Transfer transfer;

    @JsonCreator
    public TransferRequiresManualAcceptation(@JsonProperty("transfer") Transfer transfer) {
      this.transfer = transfer;
    }
  }
}
