package com.example.actions;

import com.example.CounterEvent;
import com.example.CounterEvent.ValueIncreased;
import com.example.CounterEvent.ValueMultiplied;
import com.example.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//TODO move to type level the subscription when SDK is released
// tag::sub-ESE-action[]
// tag::sub-topic-action[]
import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.Subscribe;

public class CounterJournalToTopicAction extends Action {

    private Logger logger = LoggerFactory.getLogger(CounterJournalToTopicAction.class);

    // end::sub-ESE-action[]
    // end::sub-topic-action[]

     // tag::sub-ESE-action[]
     @Subscribe.EventSourcedEntity(value = Counter.class) // <1>
     public Action.Effect<Integer> onEntityEvent(CounterEvent event){ // <2>
         if (event instanceof ValueIncreased){
             logger.info("Received increased event: " + event.toString());
         } else if (event instanceof ValueMultiplied){
             logger.info("Received multiplied event: " + event.toString());
         }
         return effects().reply(0); // <3>
     }
     // end::sub-ESE-action[]

    // tag::sub-topic-action[]
    @Subscribe.Topic(value = "counter-events") // <1>
    public Action.Effect<Integer> onTopicEvent(CounterEvent event){ // <2>
        if (event instanceof ValueIncreased){
            logger.info("Received increased event: " + event.toString());
        } else if (event instanceof ValueMultiplied){
            logger.info("Received multiplied event: " + event.toString());
        }
        return effects().reply(0); // <3>
    }
    // end::sub-topic-action[]
// tag::sub-ESE-action[]
// tag::sub-topic-action[]
}
// end::sub-topic-action[]
// end::sub-ESE-action[]