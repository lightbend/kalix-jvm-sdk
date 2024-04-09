/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TransferState {
  public final Transfer transfer;
  public final String lastStep;
  public final boolean finished;
  public final boolean accepted;

  public TransferState(Transfer transfer, String lastStep) {
    this(transfer, lastStep, false, false);
  }

  @JsonCreator
  public TransferState(@JsonProperty("transfer") Transfer transfer,
                       @JsonProperty("lastStep") String lastStep,
                       @JsonProperty("finished") boolean finished,
                       @JsonProperty("accepted") boolean accepted) {
    this.transfer = transfer;
    this.lastStep = lastStep;
    this.finished = finished;
    this.accepted = accepted;
  }

  public TransferState withLastStep(String lastStep) {
    return new TransferState(transfer, lastStep);
  }

  public TransferState accepted() {
    return new TransferState(transfer, lastStep, finished, true);
  }

  public TransferState finished() {
    return new TransferState(transfer, lastStep, true, accepted);
  }
}
