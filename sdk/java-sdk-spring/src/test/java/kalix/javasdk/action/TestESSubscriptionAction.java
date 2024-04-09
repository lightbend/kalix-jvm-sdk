/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.action;

import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.eventsourcedentity.TestESEvent;
import kalix.javasdk.eventsourcedentity.TestEventSourcedEntity;

@Subscribe.EventSourcedEntity(value = TestEventSourcedEntity.class, ignoreUnknown = true)
public class TestESSubscriptionAction extends Action {

  public Effect<Integer> handleEvent2(TestESEvent.Event2 event) {
    return effects().reply(event.newName());
  }

  public Effect<Boolean> handleEvent3(TestESEvent.Event3 event) {
    return effects().reply(event.b());
  }

  public Effect<String> handleEvent4(TestESEvent.Event4 event) {
    return effects().reply(event.anotherString());
  }
}
