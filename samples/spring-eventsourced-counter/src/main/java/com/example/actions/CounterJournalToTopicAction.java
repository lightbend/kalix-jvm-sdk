package com.example.actions;



import com.example.CounterEvent;
import com.example.CounterEvent.ValueIncreased;
import com.example.CounterEvent.ValueMultiplied;
import com.example.Counter;

// tag::class[]
import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.Publish;
import kalix.springsdk.annotations.Subscribe;

public class CounterJournalToTopicAction extends Action {

    @Subscribe.EventSourcedEntity(value = Counter.class) // <1>
    @Publish.Topic("counter-events") // <2>
    public Action.Effect<Confirmed> onEntityEventToTopic(CounterEvent event){ // <3>
        if (event instanceof ValueIncreased){
            ValueIncreased vi = new ValueIncreased(((ValueIncreased) event).value() + 1);
            return effects().reply(Confirmed.defaultInstance());
        } else if (event instanceof ValueMultiplied){
            ValueMultiplied vm = new ValueMultiplied(((ValueMultiplied) event).value()*2);
            return effects().reply(Confirmed.defaultInstance());
        } else return effects().reply(Confirmed.defaultInstance()); // <4>
    }
}
// end::class[]