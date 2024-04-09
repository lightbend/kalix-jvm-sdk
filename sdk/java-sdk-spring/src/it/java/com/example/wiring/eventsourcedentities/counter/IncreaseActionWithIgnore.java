/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.eventsourcedentities.counter;

import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;

import java.util.concurrent.CompletionStage;

@Subscribe.EventSourcedEntity(value = CounterEntity.class, ignoreUnknown = true)
public class IncreaseActionWithIgnore extends Action {

    private ComponentClient componentClient;

    private ActionCreationContext context;

    public IncreaseActionWithIgnore(ComponentClient componentClient, ActionCreationContext context) {
        this.componentClient = componentClient;
        this.context = context;
    }

    public Effect<Integer> oneShallPass(CounterEvent.ValueIncreased event) {
        String entityId = this.actionContext().metadata().asCloudEvent().subject().get();
        if (event.value() == 1234) {
            CompletionStage<Integer> res =
                componentClient.forEventSourcedEntity(entityId).call(CounterEntity::increase).params(1).execute();
            return effects().asyncReply(res);
        }
        return effects().reply(event.value());
    }
}