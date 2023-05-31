package com.example.actions;

import com.example.actions.CounterTopicApi.Increased;
import com.example.domain.CounterDomain;
import com.example.domain.CounterDomain.ValueIncreased;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import kalix.javasdk.Metadata;
import kalix.javasdk.action.ActionCreationContext;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your com/example/actions/counter_topic.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::class[]
public class CounterJournalToTopicWithMetaAction extends AbstractCounterJournalToTopicWithMetaAction {

  public CounterJournalToTopicWithMetaAction(ActionCreationContext creationContext) {}

  @Override
  public Effect<Increased> onIncreased(ValueIncreased valueIncreased) {
    Increased increased = Increased.newBuilder().setValue(valueIncreased.getValue()).build();
    String counterId = actionContext().metadata().get("ce-subject").orElseThrow(); // <1>
    Metadata metadata = Metadata.EMPTY.add("ce-subject", counterId);
    return effects().reply(increased, metadata); // <2>
  }
  // end::class[]

  @Override
  public Effect<CounterTopicApi.Decreased> onDecreased(CounterDomain.ValueDecreased valueDecreased) {
    CounterTopicApi.Decreased decreased = CounterTopicApi.Decreased.newBuilder().setValue(valueDecreased.getValue()).build();
    String counterId = actionContext().metadata().get("ce-subject").orElseThrow();
    Metadata metadata = Metadata.EMPTY.add("ce-subject", counterId);
    return effects().reply(decreased, metadata);
  }
  // tag::class[]
}
// end::class[]
