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

import com.example.wiring.actions.echo.Message;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.workflow.Workflow;
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
import static kalix.javasdk.workflow.AbstractWorkflow.RecoverStrategy.maxRetries;

@Id("workflowId")
@TypeId("workflow-with-timeout")
@RequestMapping("/workflow-with-timeout/{workflowId}")
public class WorkflowWithTimeout extends Workflow<FailingCounterState> {

  private final String counterStepName = "counter";
  private final String counterFailoverStepName = "counter-failover";

  private ComponentClient componentClient;

  public WorkflowWithTimeout(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }


  public Executor delayedExecutor = CompletableFuture.delayedExecutor(1, TimeUnit.SECONDS);

  @Override
  public WorkflowDef<FailingCounterState> definition() {
    var counterInc =
        step(counterStepName)
            .asyncCall(() -> CompletableFuture.supplyAsync(() -> "nothing", delayedExecutor))
            .andThen(String.class, __ -> effects().end())
            .timeout(Duration.ofMillis(50));

    var counterIncFailover =
        step(counterFailoverStepName)
            .call(Integer.class, value -> componentClient.forEventSourcedEntity(currentState().counterId()).call(FailingCounterEntity::increase).params(value))
            .andThen(Integer.class, __ ->
                effects()
                    .updateState(currentState().asFinished())
                    .transitionTo(counterStepName)
            );


    return workflow()
        .timeout(ofSeconds(1))
        .defaultStepTimeout(ofMillis(999))
        .failoverTo(counterFailoverStepName, 3, maxRetries(1))
        .addStep(counterInc, maxRetries(1).failoverTo(counterStepName))
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
