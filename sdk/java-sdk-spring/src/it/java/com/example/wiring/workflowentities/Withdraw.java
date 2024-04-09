/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Withdraw {
  public final String from;
  public final int amount;

  @JsonCreator
  public Withdraw(@JsonProperty("from") String from, @JsonProperty("amount") int amount) {
    this.from = from;
    this.amount = amount;
  }
}
