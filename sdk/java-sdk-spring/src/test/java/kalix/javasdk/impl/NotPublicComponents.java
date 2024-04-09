/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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

