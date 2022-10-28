package com.example.actions;

// tag::sub-ESE-pub-topic-action[]
import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.Publish;
import kalix.springsdk.annotations.Subscribe;

// end::sub-ESE-pub-topic-action[]

import com.example.CounterEvent;
import com.example.CounterEvent.ValueIncreased;
import com.example.CounterEvent.ValueMultiplied;
import com.example.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//TODO move to type level the subscription when SDK is released

// tag::sub-ESE-pub-topic-action[]
public class CounterJournalToTopicActionPubSub extends Action {

    @Subscribe.EventSourcedEntity(value = Counter.class) // <1>
    @Publish.Topic("counter-events") // <2>
    public Action.Effect<CounterEvent> onEntityEventToTopic(CounterEvent event){ // <3>
        if (event instanceof ValueIncreased){
            ValueIncreased vi = new ValueIncreased(((ValueIncreased) event).value() + 1);
            return effects().reply(vi);
        } else if (event instanceof ValueMultiplied){
            ValueMultiplied vm = new ValueMultiplied(((ValueMultiplied) event).value()*2);
            return effects().reply(vm);
        } else return effects().reply(event); // <4>
    }
}
// end::sub-ESE-pub-topic-action[]