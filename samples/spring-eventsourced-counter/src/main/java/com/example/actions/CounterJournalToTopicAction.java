package com.example.actions;

// tag::pubsub-action[]
import kalix.javasdk.action.Action;
import kalix.springsdk.annotations.Publish;
import kalix.springsdk.annotations.Subscribe;

// end::pubsub-action[]
import com.example.CounterEvent;
import com.example.CounterEvent.ValueIncreased;
import com.example.CounterEvent.ValueMultiplied;
import com.example.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//TODO move to type level the subscription when SDK is released

// tag::pubsub-action[]
public class CounterJournalToTopicAction extends Action {
    private Logger logger = LoggerFactory.getLogger(CounterJournalToTopicAction.class);
    // tag::pubsub-action[]
    @Subscribe.EventSourcedEntity(value = Counter.class) // <1>
    @Publish.Topic("increasedTopic") // <2>
    public Action.Effect<CounterEvent> onValueIncreased(ValueIncreased valueIncreased){ // <3>
       ValueIncreased vi = new ValueIncreased(valueIncreased.value() + 1);
       return effects().reply(vi);
    }
    // end::pubsub-action[]
    @Subscribe.EventSourcedEntity(value = Counter.class) // <4>
    public Action.Effect<Integer> onValueMultiplied(ValueMultiplied valueMultiplied){ // <5>
        logger.info("Received multiplied event: " + valueMultiplied.toString()); // <6>
        return effects().reply(0);
    }

    @Subscribe.Topic("increasedTopic") // <7>
    public Action.Effect<Integer> onEvent(ValueIncreased valueIncreased){
        logger.info("Received increase event: " + valueIncreased.toString()); // <8>
        return effects().reply(0);  // <9>
    }
}
// end::pubsub-action[]
