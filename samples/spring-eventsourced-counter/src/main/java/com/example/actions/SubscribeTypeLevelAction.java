package com.example.actions;

import com.example.CounterEvent;
import com.example.CounterEvent.ValueIncreased;
import com.example.CounterEvent.ValueMultiplied;
import com.example.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// tag::class[]
import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.Subscribe;

@Subscribe.EventSourcedEntity(value = Counter.class, ignoreUnkown = true) // <1>
public class SubscribeTypeLevelAction extends Action {

    private Logger logger = LoggerFactory.getLogger(SubscribeTypeLevelAction.class);

    public Action.Effect<Integer> onIncrease(ValueIncreased event){ // <2>
        logger.info("Received increased event: " + event.toString());
        return effects().reply(0); // <3>
    }
}
// end::class[]