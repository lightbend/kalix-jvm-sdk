/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.eventsourcedentities.tracingcounter;

import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.annotations.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TIncreaseAction extends Action {

    Logger log = LoggerFactory.getLogger(TIncreaseAction.class);

    private ActionCreationContext actionCreationContext;

    public TIncreaseAction(ActionCreationContext actionCreationContext){
        this.actionCreationContext = actionCreationContext;
    }

    @Subscribe.EventSourcedEntity(value = TCounterEntity.class)
    public Effect<Integer> printIncrease(TCounterEvent.ValueIncreased increase){
        log.info("increasing [{}].", increase);
        return effects().reply(increase.value());
    }



}
