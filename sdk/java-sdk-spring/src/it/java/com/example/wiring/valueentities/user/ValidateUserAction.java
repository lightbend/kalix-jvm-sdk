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
