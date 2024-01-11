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

package kalix.javasdk.impl;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.*;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.view.View;
import kalix.javasdk.workflow.Workflow;
import kalix.spring.testmodels.Message;
import kalix.spring.testmodels.valueentity.User;
import kalix.spring.testmodels.valueentity.UserEntity;
import kalix.spring.testmodels.workflow.StartWorkflow;
import kalix.spring.testmodels.workflow.WorkflowState;
import org.springframework.web.bind.annotation.*;

// below components are not public and thus need to be in the same package as the corresponding test
public class NotPublicComponents {
  static class NotPublicAction extends Action {
    @GetMapping("/message")
    public Action.Effect<Message> message() {
      return effects().ignore();
    }
  }

  @Id("counter_id")
  @TypeId("counter")
  static class NotPublicEventSourced extends EventSourcedEntity<Integer, Object> {
    @GetMapping("/eventsourced/{counter_id}")
    public Integer test() {
      return 0;
    }
  }

  @Id("id")
  @TypeId("user")
  @RequestMapping("/user/{id}")
  static class NotPublicValueEntity extends ValueEntity<User> {

    @GetMapping
    public ValueEntity.Effect<String> ok() {
      return effects().reply("ok");
    }
  }

  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  static class NotPublicView extends View<User> {
    @Query("SELECT * FROM users_view WHERE email = :email")
    @GetMapping("/users/{email}")
    public User getUser(@PathVariable String email) {
      return null;
    }
  }

  @TypeId("transfer-workflow")
  @Id("transferId")
  @RequestMapping("/transfer/{transferId}")
  static class NotPublicWorkflow extends Workflow<WorkflowState> {
    @Override
    public WorkflowDef<WorkflowState> definition() {
      return null;
    }

    @PutMapping
    public Effect<String> startTransfer(@RequestBody StartWorkflow startWorkflow) {
      return null;
    }
  }
}

