/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
