/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.workflow.Workflow;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("id")
@TypeId("dummy-workflow")
@RequestMapping("/dummy-workflow/{id}")
public class DummyWorkflow extends Workflow<Integer> {

  @Override
  public WorkflowDef<Integer> definition() {
    return workflow();
  }

  @PostMapping
  public Effect<String> startAndFinish() {
    return effects().updateState(10).end().thenReply("ok");
  }

  @PatchMapping("/test")
  public Effect<String> update() {
    return effects().updateState(20).transitionTo("asd").thenReply("ok");
  }

  @GetMapping
  public Effect<Integer> get() {
    return effects().reply(currentState());
  }
}
