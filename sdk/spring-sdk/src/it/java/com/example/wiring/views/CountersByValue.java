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

package com.example.wiring.views;

import com.example.wiring.eventsourcedentities.counter.Counter;
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Table;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Table("counters_by_value")
public class CountersByValue extends View<Counter> {

  @GetMapping("/counters-by-value/{value}")
  @Query("SELECT * FROM counters_by_value WHERE value = :value")
  public Counter getCounterByValue(@PathVariable Integer value) {
    return null;
  }

  //  @Subscribe.EventSourcedEntity( CounterEntity.class)
  //  public Counter onEvent(Counter counter, CounterEvent event) {
  //
  //    if (event instanceof ValueIncreased) {
  //      return counter.onValueIncreased((ValueIncreased) event);
  //    } else if (event instanceof ValueMultiplied) {
  //      return counter.onValueMultiplied((ValueMultiplied) event);
  //    } else {
  //      return counter;
  //    }
  //  }

}
