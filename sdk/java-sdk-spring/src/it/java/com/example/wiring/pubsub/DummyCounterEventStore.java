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
