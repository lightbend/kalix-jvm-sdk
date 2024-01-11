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
