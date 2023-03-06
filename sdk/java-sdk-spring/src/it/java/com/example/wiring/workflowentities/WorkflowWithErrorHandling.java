/*
 * Copyright 2021 Lightbend Inc.
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

import com.example.wiring.actions.echo.Message;
import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.workflowentity.WorkflowEntity;
import kalix.spring.KalixClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static java.time.Duration.ofSeconds;
import static kalix.javasdk.workflowentity.WorkflowEntity.RecoverStrategy.maxRetries;

@EntityType("workflow-with-error-handling")
@EntityKey("workflowId")
@RequestMapping("/workflow-with-error-handling/{workflowId}")
public class WorkflowWithErrorHandling extends WorkflowEntity<WorkflowWithErrorHandling.FailingCounterState> {

  public record FailingCounterState(String counterId, int value, boolean finished) {
    public FailingCounterState asFinished() {
      return new FailingCounterState(counterId, value, true);
    }

    public FailingCounterState inc() {
      return new FailingCounterState(counterId, value + 1, finished);
    }
  }

  private final String counterStepName = "counter";
  private final String counterFailoverStepName = "counter-failover";

  private KalixClient kalixClient;

  public WorkflowWithErrorHandling(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }


  @Override
  public Workflow<WorkflowWithErrorHandling.FailingCounterState> definition() {
    var counterInc =
        step(counterStepName)
            .call(() -> {
              var nextValue = currentState().value() + 1;
              return kalixClient.post("/failing-counter/" + currentState().counterId + "/increase/" + nextValue, Integer.class);
            })
            .andThen(__ -> effects()
                .updateState(currentState().asFinished())
                .end())
            .timeout(ofSeconds(1))
            .recoveryStrategy(maxRetries(1).failoverTo(counterFailoverStepName));

    var counterIncFailover =
        step(counterFailoverStepName)
            .asyncCall(() -> CompletableFuture.completedStage("nothing"))
            .andThen(__ ->
                effects()
                    .updateState(currentState().inc())
                    .transitionTo(counterStepName)
            );


    return workflow()
        .timeout(ofSeconds(30))
        .stepTimeout(ofSeconds(10))
        .stepRecoveryStrategy(maxRetries(1).failoverTo(counterStepName))
        .addStep(counterInc)
        .addStep(counterIncFailover);
  }

  @PutMapping("/{counterId}")
  public Effect<Message> startFailingCounter(@PathVariable String counterId) {
    return effects()
        .updateState(new FailingCounterState(counterId, 0, false))
        .transitionTo(counterStepName)
        .thenReply(new Message("workflow started"));
  }
}
