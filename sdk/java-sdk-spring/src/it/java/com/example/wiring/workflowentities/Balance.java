/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.workflowentities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Balance {
  public final int value;

  @JsonCreator
  public Balance(@JsonProperty("value") int value) {
    this.value = value;
  }
}
