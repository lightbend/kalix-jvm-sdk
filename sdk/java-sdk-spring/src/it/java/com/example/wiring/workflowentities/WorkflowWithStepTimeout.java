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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;
import static kalix.javasdk.workflowentity.WorkflowEntity.RecoverStrategy.maxRetries;

@EntityType("workflow-with-step-timeout")
@EntityKey("workflowId")
@RequestMapping("/workflow-with-step-timeout/{workflowId}")
public class WorkflowWithStepTimeout extends WorkflowEntity<FailingCounterState> {

  private final String counterStepName = "counter";
  private final String counterFailoverStepName = "counter-failover";

  private KalixClient kalixClient;

  public WorkflowWithStepTimeout(KalixClient kalixClient) {
    this.kalixClient = kalixClient;
  }


  public Executor delayedExecutor = CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS);

  @Override
  public Workflow<FailingCounterState> definition() {
    var counterInc =
        step(counterStepName)
            .asyncCall(() -> CompletableFuture.supplyAsync(() -> "produces time out", delayedExecutor))
            .andThen(__ -> effects().transitionTo(counterFailoverStepName))
            .timeout(ofMillis(20));

    var counterIncFailover =
        step(counterFailoverStepName)
            .asyncCall(() -> CompletableFuture.completedStage("nothing"))
            .andThen(__ -> effects()
                .updateState(currentState().inc())
                .transitionTo(counterStepName));


    return workflow()
        .timeout(ofSeconds(10))
        .stepTimeout(ofMillis(20))
        .addStep(counterInc, maxRetries(2).failoverTo(counterFailoverStepName))
        .addStep(counterIncFailover);
  }

  @PutMapping("/{counterId}")
  public Effect<Message> startFailingCounter(@PathVariable String counterId) {
    return effects()
        .updateState(new FailingCounterState(counterId, 0, false))
        .transitionTo(counterStepName)
        .thenReply(new Message("workflow started"));
  }

  @GetMapping
  public Effect<FailingCounterState> get() {
    return effects().reply(currentState());
  }
}
