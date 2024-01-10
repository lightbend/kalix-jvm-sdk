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
