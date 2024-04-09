/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.valueentities.user;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;

public class SubscribeUserAction extends Action {

  @Subscribe.ValueEntity(UserEntity.class)
  public Action.Effect<String> onUpdate(User user) {
    String userId = actionContext().metadata().get("ce-subject").get();
    UserSideEffect.addUser(userId, user);
    return effects().ignore();
  }

  @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
  public Action.Effect<String> onDelete() {
    String userId = actionContext().metadata().get("ce-subject").get();
    UserSideEffect.removeUser(userId);
    return effects().ignore();
  }
}
