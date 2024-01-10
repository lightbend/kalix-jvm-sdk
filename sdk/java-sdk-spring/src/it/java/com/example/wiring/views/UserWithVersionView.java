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

package com.example.wiring.views;

import com.example.wiring.valueentities.user.User;
import com.example.wiring.valueentities.user.UserEntity;
import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Table("user_view")
public class UserWithVersionView extends View<UserWithVersion> {

  @Subscribe.ValueEntity(UserEntity.class)
  public UpdateEffect<UserWithVersion> onChange(User user) {
    if (viewState() == null) return effects().updateState(new UserWithVersion(user.email, 1));
    else return effects().updateState(new UserWithVersion(user.email, viewState().version + 1));
  }

  @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
  public UpdateEffect<UserWithVersion> onDelete() {
    return effects().deleteState();
  }

  @Query("SELECT * FROM user_view WHERE email = :email")
  @GetMapping("/users/by-email/{email}")
  public UserWithVersion getUser(@PathVariable String email) {
    return null;
  }
}
