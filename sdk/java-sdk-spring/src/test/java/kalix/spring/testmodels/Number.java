/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Number {

  public final int value;

  @JsonCreator
  public Number(@JsonProperty("value") int value) {
    this.value = value;
  }
}
