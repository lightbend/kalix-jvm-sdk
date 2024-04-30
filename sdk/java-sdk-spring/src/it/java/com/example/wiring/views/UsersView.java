/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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

@Table("users")
@Subscribe.ValueEntity(UserEntity.class)
public class UsersView extends View<User> {

  @GetMapping("/users/by_email/{email}")
  @Query("SELECT * FROM users WHERE email = :email")
  public User getUsersEmail(@PathVariable String email) {
    return null;
  }

  @GetMapping("/users/by_name/{name}")
  @Query("SELECT * FROM users WHERE name = :name")
  public User getUsersByName(@PathVariable String name) {
    return null;
  }
}
