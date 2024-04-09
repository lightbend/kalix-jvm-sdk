/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Message2 {

  public final String value;

  @JsonCreator
  public Message2(@JsonProperty("value") String value) {
    this.value = value;
  }
}
