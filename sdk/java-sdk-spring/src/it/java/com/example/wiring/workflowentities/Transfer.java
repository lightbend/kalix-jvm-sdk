/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Transfer {
  public final String from;
  public final String to;
  public final int amount;

  @JsonCreator
  public Transfer(@JsonProperty("from") String from, @JsonProperty("to") String to, @JsonProperty("amount") int amount) {
    this.from = from;
    this.to = to;
    this.amount = amount;
  }
}
