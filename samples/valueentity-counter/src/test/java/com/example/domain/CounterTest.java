package com.example.domain;

import com.akkaserverless.javasdk.reply.ErrorReply;
import com.akkaserverless.javasdk.valueentity.ValueEntityBase;
import com.example.CounterApi;
import com.google.protobuf.Empty;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

// tag::class[]
public class CounterTest {
    private final String entityId = "entityId1";
    private Counter entity;
// end::class[]
    private static class MockedContextFailure extends RuntimeException {};

    // tag::increase[] 
    @Test
    public void increaseNoPriorState() {
        entity = new Counter(entityId); // <1>

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(42).build(); // <2>
        entity.increase(currentState, message); // <3>

        // FIXME Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(42).build()); // <4>
    }
    // end::increase[]
    @Test
    public void increaseWithPriorState() {
        entity = new Counter(entityId);

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(42).build();
        entity.increase(currentState, message);

        // FIXME Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(13 + 42).build());
    }

    @Test
    public void increaseShouldFailWithNegativeValue() {
        entity = new Counter(entityId);

        assertThrows(MockedContextFailure.class, () -> {
            CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(-2).build();
            entity.increase(message, context);
        });
    }

    @Test
    public void decreaseNoPriorState() {
        entity = new Counter(entityId);

        CounterApi.DecreaseValue message = CounterApi.DecreaseValue.newBuilder().setValue(42).build();
        entity.decrease(currentState, message);

        // FIXME Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(-42).build());
    }
    
    @Test
    public void resetTest() {
        entity = new Counter(entityId);

        CounterApi.ResetValue message = CounterApi.ResetValue.newBuilder().build();
        entity.reset(currentState, message);

        // FIXME Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(0).build());
    }
    
    @Test
    public void getCurrentCounterTest() {
        entity = new Counter(entityId);

        CounterDomain.CounterState currentState = CounterDomain.CounterState.newBuilder().setValue(13).build();

        CounterApi.GetCounter message = CounterApi.GetCounter.newBuilder().build();
        ValueEntityBase.Effect<CounterApi.CurrentCounter> reply = entity.getCurrentCounter(currentState, message);

        // FIXME assertThat(((MessageReply<CounterApi.CurrentCounter>) reply).payload().getValue(), is(13));
    }


}
