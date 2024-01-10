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
