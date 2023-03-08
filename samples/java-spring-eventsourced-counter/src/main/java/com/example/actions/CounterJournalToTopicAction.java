package com.example.actions;



import com.example.CounterEvent;
import com.example.CounterEvent.ValueIncreased;
import com.example.CounterEvent.ValueMultiplied;
import com.example.Counter;

// tag::class[]
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Subscribe;

public class CounterJournalToTopicAction extends Action {

    @Subscribe.EventSourcedEntity(value = Counter.class) // <1>
    @Publish.Topic("counter-events") // <2>
    public Action.Effect<ValueIncreased> onValueIncreased(ValueIncreased event){ // <3>
        ValueIncreased vi = new ValueIncreased(event.value() + 1);
        return effects().reply(vi); // <4>
    }
    // end::class[]

    @Subscribe.EventSourcedEntity(value = Counter.class)
    @Publish.Topic("counter-events")
    public Action.Effect<ValueIncreased> onValueMultiplied(ValueMultiplied event){
        ValueIncreased vi = new ValueIncreased(event.value() + 1);
        return effects().reply(vi);
    }
// tag::class[]
}
// end::class[]