/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import kalix.javasdk.annotations.Migration;

import java.util.Objects;

@Migration(DummyClass2Migration.class)
public class DummyClass2 {
  public String stringValue;
  public int intValue;
  public String mandatoryStringValue;

  @JsonCreator
  public DummyClass2(String stringValue, int intValue, String mandatoryStringValue) {
    this.stringValue = stringValue;
    this.intValue = intValue;
    this.mandatoryStringValue = mandatoryStringValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DummyClass2 that = (DummyClass2) o;
    return intValue == that.intValue && Objects.equals(stringValue, that.stringValue) && Objects.equals(mandatoryStringValue, that.mandatoryStringValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stringValue, intValue, mandatoryStringValue);
  }

  @Override
  public String toString() {
    return "DummyClass2{" +
        "stringValue='" + stringValue + '\'' +
        ", intValue=" + intValue +
        ", mandatoryStringValue='" + mandatoryStringValue + '\'' +
        '}';
  }
}
