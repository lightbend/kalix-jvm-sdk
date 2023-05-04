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

import akka.Done;
import com.example.wiring.actions.echo.Message;
import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.workflowentity.WorkflowEntity;
import kalix.javasdk.workflowentity.WorkflowEntityContext;
import kalix.spring.KalixClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.Duration;

@EntityType("workflow-with-timer")
@EntityKey("workflowId")
@RequestMapping("/workflow-with-timer/{workflowId}")
public class WorkflowWithTimer extends WorkflowEntity<FailingCounterState> {

  private final String counterStepName = "counter";

  private final KalixClient kalixClient;
  private final WorkflowEntityContext workflowEntityContext;

  public WorkflowWithTimer(KalixClient kalixClient, WorkflowEntityContext workflowEntityContext) {
    this.kalixClient = kalixClient;
    this.workflowEntityContext = workflowEntityContext;
  }

  @Override
  public Workflow<FailingCounterState> definition() {
    var counterInc =
        step(counterStepName)
            .asyncCall(() -> {
              var pingWorkflow = kalixClient.put("/workflow-with-timer/" + workflowEntityContext.entityId() + "/ping",
                  new CounterScheduledValue(12),
                  String.class);
              return timers().startSingleTimer("ping", Duration.ofSeconds(2), pingWorkflow);
            })
            .andThen(Done.class, __ -> effects().pause())
            .timeout(Duration.ofMillis(50));


    return workflow()
        .addStep(counterInc);
  }

  @PutMapping("/{counterId}")
  public Effect<Message> startFailingCounter(@PathVariable String counterId) {
    return effects()
        .updateState(new FailingCounterState(counterId, 0, false))
        .transitionTo(counterStepName)
        .thenReply(new Message("workflow started"));
  }

  @PutMapping("/ping")
  public Effect<String> pingWorkflow(@RequestBody CounterScheduledValue counterScheduledValue) {
    return effects()
        .updateState(currentState().asFinished(counterScheduledValue.value()))
        .end()
        .thenReply("workflow finished");
  }

  @GetMapping
  public Effect<FailingCounterState> get() {
    return effects().reply(currentState());
  }
}
