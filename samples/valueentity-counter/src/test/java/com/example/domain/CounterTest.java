package com.example.domain;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.reply.FailureReply;
import com.akkaserverless.javasdk.reply.MessageReply;
import com.example.CounterApi;
import com.google.protobuf.Empty;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

// tag::class[]
public class CounterTest {
    private final String entityId = "entityId1";
    private CounterImpl entity;
// end::class[]
    private static class MockedContextFailure extends RuntimeException {};

    // tag::increase[] 
    @Test
    public void increaseNoPriorState() {
        entity = new CounterImpl(entityId); // <1>

        CounterDomain.CounterState currentState = CounterDomain.CounterState.getDefaultInstance();

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(42).build(); // <3>
        entity.increase(currentState, message); // <4>

        // FIXME Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(42).build()); // <5>
    }
    // end::increase[]
    @Test
    public void increaseWithPriorState() {
        entity = new CounterImpl(entityId);

        CounterDomain.CounterState currentState = CounterDomain.CounterState.newBuilder().setValue(13).build();

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(42).build();
        entity.increase(currentState, message);

        // FIXME Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(13 + 42).build());
    }

    @Test
    public void increaseShouldFailWithNegativeValue() {
        entity = new CounterImpl(entityId);

        CounterDomain.CounterState currentState = CounterDomain.CounterState.newBuilder().setValue(27).build();

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(-2).build();
        Effect<Empty> reply = entity.increase(currentState, message);
        assertThat(reply,  is(instanceOf(FailureReply.class)));
    }

    @Test
    public void decreaseNoPriorState() {
        entity = new CounterImpl(entityId);

        CounterDomain.CounterState currentState = CounterDomain.CounterState.getDefaultInstance();

        CounterApi.DecreaseValue message = CounterApi.DecreaseValue.newBuilder().setValue(42).build();
        entity.decrease(currentState, message);

        // FIXME Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(-42).build());
    }
    
    @Test
    public void resetTest() {
        entity = new CounterImpl(entityId);

        CounterDomain.CounterState currentState = CounterDomain.CounterState.newBuilder().setValue(13).build();

        CounterApi.ResetValue message = CounterApi.ResetValue.newBuilder().build();
        entity.reset(currentState, message);

        // FIXME Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(0).build());
    }
    
    @Test
    public void getCurrentCounterTest() {
        entity = new CounterImpl(entityId);

        CounterDomain.CounterState currentState = CounterDomain.CounterState.newBuilder().setValue(13).build();

        CounterApi.GetCounter message = CounterApi.GetCounter.newBuilder().build();
        Effect<CounterApi.CurrentCounter> reply = entity.getCurrentCounter(currentState, message);

        assertThat(((MessageReply<CounterApi.CurrentCounter>) reply).payload().getValue(), is(13));
    }

}
