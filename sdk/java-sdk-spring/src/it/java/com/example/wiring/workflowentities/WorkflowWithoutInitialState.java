/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.client.ComponentClient;
import kalix.javasdk.workflow.Workflow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.CompletableFuture;

@Id("id")
@TypeId("workflow-without-initial-state")
@RequestMapping("/workflow-without-initial-state/{id}")
public class WorkflowWithoutInitialState extends Workflow<String> {


  private ComponentClient componentClient;

  public WorkflowWithoutInitialState(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  @Override
  public WorkflowDef<String> definition() {
    var test =
        step("test")
            .asyncCall(() -> CompletableFuture.completedFuture("ok"))
            .andThen(String.class, result -> effects().updateState("success").end());

    return workflow()
        .addStep(test);
  }

  @PutMapping()
  public Effect<String> start() {
    return effects().transitionTo("test").thenReply("ok");
  }

  @GetMapping()
  public Effect<String> get() {
    if (currentState() == null) {
      return effects().reply("empty");
    } else {
      return effects().reply(currentState());
    }
  }
}
