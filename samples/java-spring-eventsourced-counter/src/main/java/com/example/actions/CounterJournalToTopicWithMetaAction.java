package com.example.actions;

import com.example.Counter;
import com.example.CounterEvent;
import com.example.CounterEvent.ValueIncreased;
import com.example.CounterEvent.ValueMultiplied;
import kalix.javasdk.Metadata;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Subscribe;

// tag::class[]
@Subscribe.EventSourcedEntity(value = Counter.class)
public class CounterJournalToTopicWithMetaAction extends Action {

  @Publish.Topic("counter-events-with-meta")
  public Effect<CounterEvent> onValueIncreased(ValueIncreased event) {
    String counterId = actionContext().metadata().get("ce-subject").orElseThrow(); // <1>
    Metadata metadata = Metadata.EMPTY.add("ce-subject", counterId);
    return effects().reply(event, metadata); // <2>
  }
  // end::class[]

  @Publish.Topic("counter-events-with-meta")
  public Effect<CounterEvent> onValueMultiplied(ValueMultiplied event) {
    String counterId = actionContext().metadata().get("ce-subject").orElseThrow();
    Metadata metadata = Metadata.EMPTY.add("ce-subject", counterId);
    return effects().reply(event, metadata);
  }
// tag::class[]
}
// end::class[]