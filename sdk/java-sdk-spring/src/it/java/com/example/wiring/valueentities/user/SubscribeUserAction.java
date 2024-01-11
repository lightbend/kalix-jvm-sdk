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
