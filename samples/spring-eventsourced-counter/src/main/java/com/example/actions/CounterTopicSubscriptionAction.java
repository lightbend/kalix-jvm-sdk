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

public class CounterTopicSubscriptionAction extends Action {

    private Logger logger = LoggerFactory.getLogger(CounterTopicSubscriptionAction.class);

    @Subscribe.Topic(value = "counter-events") // <1>
    public Action.Effect<Confirmed> onTopicEvent(CounterEvent event){ // <2>
        if (event instanceof ValueIncreased){
            logger.info("Received increased event: " + event.toString());
        } else if (event instanceof ValueMultiplied){
            logger.info("Received multiplied event: " + event.toString());
        }
        return effects().reply(Confirmed.defaultInstance()); // <3>
    }
}
// end::class[]