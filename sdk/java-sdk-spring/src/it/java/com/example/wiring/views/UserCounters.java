/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.views;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class UserCounters {

  public final String id;
  public final String email;
  public final String name;
  public final List<UserCounter> counters;

  @JsonCreator
  public UserCounters(
      @JsonProperty("id") String id,
      @JsonProperty("email") String email,
      @JsonProperty("name") String name,
      @JsonProperty("counters") List<UserCounter> counters) {
    this.id = id;
    this.email = email;
    this.name = name;
    this.counters = counters;
  }
}
