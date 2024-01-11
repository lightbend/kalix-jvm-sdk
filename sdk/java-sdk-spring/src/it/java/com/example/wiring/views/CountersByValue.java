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

@Table("counters_by_value")
public class CountersByValue extends View<Counter> {

  @Override
  public Counter emptyState() {
    return new Counter(0);
  }

  @GetMapping("/counters/by-value/{value}")
  @Query("SELECT * FROM counters_by_value WHERE value = :value")
  public Counter getCounterByValue(@PathVariable Integer value) {
    return null;
  }

  @Subscribe.EventSourcedEntity(CounterEntity.class)
  public UpdateEffect<Counter> onEvent(CounterEvent.ValueIncreased event) {
    Counter counter = viewState();
    return effects().updateState(counter.onValueIncreased(event));
  }

  @Subscribe.EventSourcedEntity(CounterEntity.class)
  public UpdateEffect<Counter> onEvent(CounterEvent.ValueMultiplied event) {
    Counter counter = viewState();
    return effects().updateState(counter.onValueMultiplied(event));
  }

  @Subscribe.EventSourcedEntity(CounterEntity.class)
  public UpdateEffect<Counter> onEvent(CounterEvent.ValueSet event) {
    Counter counter = viewState();
    return effects().updateState(counter.onValueSet(event));
  }
}
