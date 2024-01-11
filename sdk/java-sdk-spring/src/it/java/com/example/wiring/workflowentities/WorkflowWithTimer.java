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

import akka.Done;
import com.example.wiring.actions.echo.Message;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.workflow.Workflow;
import kalix.javasdk.workflow.WorkflowContext;
import kalix.spring.KalixClient;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@TypeId("workflow-with-timer")
@Id("workflowId")
@RequestMapping("/workflow-with-timer/{workflowId}")
public class WorkflowWithTimer extends Workflow<FailingCounterState> {

  private final String counterStepName = "counter";

  private final KalixClient kalixClient;
  private final WorkflowContext workflowContext;

  public WorkflowWithTimer(KalixClient kalixClient, WorkflowContext workflowContext) {
    this.kalixClient = kalixClient;
    this.workflowContext = workflowContext;
  }

  @Override
  public WorkflowDef<FailingCounterState> definition() {
    var counterInc =
        step(counterStepName)
            .asyncCall(() -> {
              var pingWorkflow = kalixClient.put("/workflow-with-timer/" + workflowContext.workflowId() + "/ping",
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

  @PutMapping
  public Effect<Message> startFailingCounterWithReqParam(@RequestParam String counterId) {
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
