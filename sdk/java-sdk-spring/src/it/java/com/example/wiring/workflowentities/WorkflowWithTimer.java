/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import akka.Done;
import com.example.wiring.actions.echo.Message;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.workflow.Workflow;
import kalix.javasdk.workflow.WorkflowContext;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@TypeId("workflow-with-timer")
@Id("workflowId")
@RequestMapping("/workflow-with-timer/{workflowId}")
public class WorkflowWithTimer extends Workflow<FailingCounterState> {

  private final String counterStepName = "counter";

  private final WorkflowContext workflowContext;
  private final ComponentClient componentClient;

  public WorkflowWithTimer(WorkflowContext workflowContext, ComponentClient componentClient) {
    this.workflowContext = workflowContext;
    this.componentClient = componentClient;
  }

  @Override
  public WorkflowDef<FailingCounterState> definition() {
    var counterInc =
        step(counterStepName)
            .asyncCall(() -> {
              var pingWorkflow =
                  componentClient
                      .forWorkflow(workflowContext.workflowId())
                      .call(WorkflowWithTimer::pingWorkflow)
                      .params(new CounterScheduledValue(12));

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
