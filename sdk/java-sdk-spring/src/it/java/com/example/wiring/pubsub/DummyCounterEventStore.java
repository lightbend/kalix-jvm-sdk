/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.pubsub;

import com.example.wiring.eventsourcedentities.counter.CounterEvent;
import com.example.wiring.valueentities.customer.CustomerEntity;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class DummyCounterEventStore {

  private static ConcurrentHashMap<String, List<CounterEvent>> events = new ConcurrentHashMap<>();

  public static void store(String entityId, CounterEvent counterEvent) {
    events.merge(entityId, List.of(counterEvent), (exisitingList, newList) -> Stream.concat(exisitingList.stream(), newList.stream()).toList());
  }

  public static List<CounterEvent> get(String entityId) {
    if (events.containsKey(entityId)) {
      return List.copyOf(events.get(entityId));
    } else {
      return List.of();
    }
  }

  public static void clear() {
    events.clear();
  }
}
