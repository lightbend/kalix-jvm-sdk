/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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

import java.util.concurrent.CompletableFuture;

import static java.time.Duration.ofSeconds;

@Id("workflowId")
@TypeId("workflow-with-recover-strategy")
@RequestMapping("/workflow-with-recover-strategy/{workflowId}")
public class WorkflowWithRecoverStrategy extends Workflow<FailingCounterState> {

  private final String counterStepName = "counter";
  private final String counterFailoverStepName = "counter-failover";

  private ComponentClient componentClient;

  public WorkflowWithRecoverStrategy(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }


  @Override
  public WorkflowDef<FailingCounterState> definition() {
    var counterInc =
        step(counterStepName)
            .call(() -> {
              var nextValue = currentState().value() + 1;
              return componentClient.forEventSourcedEntity(currentState().counterId()).call(FailingCounterEntity::increase).params(nextValue);
            })
            .andThen(Integer.class, __ -> effects()
                .updateState(currentState().asFinished())
                .end());

    var counterIncFailover =
        step(counterFailoverStepName)
            .asyncCall(() -> CompletableFuture.completedStage("nothing"))
            .andThen(String.class, __ ->
                effects()
                    .updateState(currentState().inc())
                    .transitionTo(counterStepName)
            );


    return workflow()
        .timeout(ofSeconds(30))
        .defaultStepTimeout(ofSeconds(10))
        .addStep(counterInc, maxRetries(1).failoverTo(counterFailoverStepName))
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
  public Effect<FailingCounterState> get(){
    if (currentState() != null) {
      return effects().reply(currentState());
    } else {
      return effects().error("transfer not started");
    }
  }
}
