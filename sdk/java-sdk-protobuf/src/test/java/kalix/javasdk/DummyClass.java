/*
 * Copyright 2021 Lightbend Inc.
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

import java.util.Objects;
import java.util.Optional;

@Migration(DummyClassMigration.class)
public class DummyClass {
  public String stringValue;
  public int intValue;
  public Optional<String> optionalStringValue;

  @JsonCreator
  public DummyClass(@JsonProperty("stringValue") String stringValue, @JsonProperty("intValue") int intValue, @JsonProperty("optionalStringValue") Optional<String> optionalStringValue) {
    this.stringValue = stringValue;
    this.intValue = intValue;
    this.optionalStringValue = optionalStringValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DummyClass that = (DummyClass) o;
    return intValue == that.intValue && Objects.equals(stringValue, that.stringValue) && Objects.equals(optionalStringValue, that.optionalStringValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(stringValue, intValue, optionalStringValue);
  }
}
