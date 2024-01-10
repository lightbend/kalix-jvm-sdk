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

package kalix.spring.testmodels.view;

import com.fasterxml.jackson.annotation.JsonCreator;
import kalix.spring.testmodels.valueentity.CounterState;

import java.util.Collection;

public class EmployeeCounters {

  public final String firstName;
  public final String lastName;
  public final String email;
  public final Collection<CounterState> counters;

  @JsonCreator
  public EmployeeCounters(
      String firstName, String lastName, String email, Collection<CounterState> counters) {
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.counters = counters;
  }
}
