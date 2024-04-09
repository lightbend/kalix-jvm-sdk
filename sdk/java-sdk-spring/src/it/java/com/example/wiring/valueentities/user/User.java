/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.valueentities.user;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class User {
  public String email;
  public String name;

  @JsonCreator
  public User(@JsonProperty("email") String email, @JsonProperty("name") String name) {
    this.email = email;
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    User user = (User) o;
    return Objects.equals(email, user.email) && Objects.equals(name, user.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, name);
  }
}
