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
