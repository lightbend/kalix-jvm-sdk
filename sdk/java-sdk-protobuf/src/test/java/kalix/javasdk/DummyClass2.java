/*
 * Copyright 2024 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
