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

package com.example.wiring.views;

import com.example.wiring.eventsourcedentities.counter.*;
import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Table("counters_by_value_with_ignore")
@Subscribe.EventSourcedEntity(value = CounterEntity.class, ignoreUnknown = true)
public class CountersByValueWithIgnore extends View<Counter> {

  @Override
  public Counter emptyState() {
    return new Counter(0);
  }

  @GetMapping("/counters-ignore/by-value-with-ignore/{value}")
  @Query("SELECT * FROM counters_by_value_with_ignore WHERE value = :value")
  public Counter getCounterByValue(@PathVariable Integer value) {
    return null;
  }

  public UpdateEffect<Counter> onValueIncreased(CounterEvent.ValueIncreased event){
    Counter counter = viewState();
    return effects().updateState(counter.onValueIncreased(event));
  }
}
