/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;
import java.util.Optional;

public class DummyClassRenamed {
  public String stringValue;
  public int intValue;
  public Optional<String> optionalStringValue;

  @JsonCreator
  public DummyClassRenamed(String stringValue, int intValue, Optional<String> optionalStringValue) {
    this.stringValue = stringValue;
    this.intValue = intValue;
    this.optionalStringValue = optionalStringValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DummyClassRenamed that = (DummyClassRenamed) o;
    return intValue == that.intValue && Objects.equals(stringValue, that.stringValue) && Objects.equals(optionalStringValue, that.optionalStringValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stringValue, intValue, optionalStringValue);
  }
}
