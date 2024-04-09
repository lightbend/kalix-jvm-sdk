/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.valueentities.user;

import kalix.javasdk.StatusCode;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.client.ComponentClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/validuser/{user}")
public class ValidateUserAction extends Action {

  private ActionCreationContext ctx;
  private ComponentClient componentClient;

  public ValidateUserAction(ActionCreationContext ctx, ComponentClient componentClient) {
    this.ctx = ctx;
    this.componentClient = componentClient;
  }

  @PutMapping("/{email}/{name}")
  public Action.Effect<String> createOrUpdateUser(@PathVariable String user, @PathVariable String email, @PathVariable String name) {
    if (email.isEmpty() || name.isEmpty())
      return effects().error("No field can be empty", StatusCode.ErrorCode.BAD_REQUEST);

    var defCall = componentClient.forValueEntity(user).call(UserEntity::createUser).params(email, name);
    return effects().forward(defCall);
  }

  @PatchMapping("/email/{email}")
  public Action.Effect<String> updateEmail(@PathVariable String user, @PathVariable String email) {
    if (email.isEmpty())
      return effects().error("No field can be empty", StatusCode.ErrorCode.BAD_REQUEST);

    var defCall = componentClient.forValueEntity(user).call(UserEntity::updateEmail).params(email);
    return effects().forward(defCall);
  }

  @DeleteMapping
  public Action.Effect<String> delete(@PathVariable String user) {
    var defCall = componentClient.forValueEntity(user).call(UserEntity::deleteUser);
    return effects().forward(defCall);
  }
}
