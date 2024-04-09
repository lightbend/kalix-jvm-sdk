/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.views;

import com.example.wiring.valueentities.user.User;
import com.example.wiring.valueentities.user.UserEntity;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.view.View;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Table("users_by_email_and_name")
@Subscribe.ValueEntity(UserEntity.class)
public class UsersByEmailAndName extends View<User> {

  @GetMapping("/users/by_email_and_name/{email}/{name}")
  @Query("SELECT * FROM users_by_email_and_name WHERE email = :email AND name = :name")
  public User getUsers(@PathVariable String email, @PathVariable String name) {
    return null;
  }
}
