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
import reactor.core.publisher.Flux;

@Table("users_by_name")
@Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
public class UsersByName extends View<User> {

  @GetMapping("/users/by-name/{name}")
  @Query("SELECT * FROM users_by_name WHERE name = :name")
  public Flux<User> getUsers(String name) {
    return null;
  }
}
