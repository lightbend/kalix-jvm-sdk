/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
