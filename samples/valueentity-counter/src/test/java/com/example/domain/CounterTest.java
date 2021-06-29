package com.example.domain;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.reply.FailureReply;
import com.akkaserverless.javasdk.reply.MessageReply;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.example.CounterApi;
import com.google.protobuf.Empty;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
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

        CommandContext<Empty, CounterDomain.CounterState> context = Mockito.mock(CommandContext.class); // <2>
        Optional<CounterDomain.CounterState> currentState = Optional.empty();

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(42).build(); // <3>
        entity.increase(currentState, message, context); // <4>

        // FIXME
        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(42).build()); // <5>
    }
    // end::increase[]
    @Test
    public void increaseWithPriorState() {
        entity = new CounterImpl(entityId);

        CommandContext<Empty, CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Optional<CounterDomain.CounterState> currentState = Optional.of(CounterDomain.CounterState.newBuilder().setValue(13).build());

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(42).build();
        entity.increase(currentState, message, context);

        // FIXME
        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(13 + 42).build());
    }

    @Test
    public void increaseShouldFailWithNegativeValue() {
        entity = new CounterImpl(entityId);

        CommandContext<Empty, CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Optional<CounterDomain.CounterState> currentState = Optional.of(CounterDomain.CounterState.newBuilder().setValue(27).build());

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(-2).build();
        Effect<Empty> reply = entity.increase(currentState, message, context);
        assertThat(reply,  is(instanceOf(FailureReply.class)));
    }

    @Test
    public void decreaseNoPriorState() {
        entity = new CounterImpl(entityId);

        CommandContext<Empty, CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Optional<CounterDomain.CounterState> currentState = Optional.empty();

        CounterApi.DecreaseValue message = CounterApi.DecreaseValue.newBuilder().setValue(42).build();
        entity.decrease(currentState, message, context);

        // FIXME
        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(-42).build());
    }
    
    @Test
    public void resetTest() {
        entity = new CounterImpl(entityId);

        CommandContext<Empty, CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Optional<CounterDomain.CounterState> currentState = Optional.of(CounterDomain.CounterState.newBuilder().setValue(13).build());

        CounterApi.ResetValue message = CounterApi.ResetValue.newBuilder().build();
        entity.reset(currentState, message, context);

        // FIXME
        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(0).build());
    }
    
    @Test
    public void getCurrentCounterTest() {
        entity = new CounterImpl(entityId);

        CommandContext<CounterApi.CurrentCounter, CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Optional<CounterDomain.CounterState> currentState = Optional.of(CounterDomain.CounterState.newBuilder().setValue(13).build());

        CounterApi.GetCounter message = CounterApi.GetCounter.newBuilder().build();
        Effect<CounterApi.CurrentCounter> reply = entity.getCurrentCounter(currentState, message, context);

        assertThat(((MessageReply<CounterApi.CurrentCounter>) reply).payload().getValue(), is(13));
    }

}
