/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.workflow;

import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.concurrent.CompletableFuture;

@Id("id")
@TypeId("workflow")
@RequestMapping("/workflow")
public class TestWorkflowSerialization extends Workflow<String> {

  @Override
  public WorkflowDef<String> definition() {
    var testStep = step("test")
        .asyncCall(() -> CompletableFuture.<Result>completedFuture(new Result.Succeed()))
        .andThen(Result.class, result -> effects().updateState("success").end());

    return workflow().addStep(testStep);
  }

  @GetMapping
  public Effect<String> start() {
    return effects()
        .updateState("empty")
        .transitionTo("test")
        .thenReply("ok");
  }

  @GetMapping
  public Effect<String> get() {
    return effects().reply(currentState());
  }
}
