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

package com.example.wiring.valueentities.user;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@TypeId("assigned-counter")
@Id("counterId")
@RequestMapping("/assigned-counter")
public class AssignedCounterEntity extends ValueEntity<AssignedCounter> {

  @Override
  public AssignedCounter emptyState() {
    return new AssignedCounter(commandContext().entityId(), "");
  }

  @PostMapping("/{counterId}/assign/{assigneeId}")
  public ValueEntity.Effect<String> assign(@PathVariable String assigneeId) {
    AssignedCounter newState = currentState().assignTo(assigneeId);
    return effects().updateState(newState).thenReply("OK");
  }
}
