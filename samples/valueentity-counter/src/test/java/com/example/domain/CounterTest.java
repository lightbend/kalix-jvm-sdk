package com.example.domain;

import com.akkaserverless.javasdk.Effect;
import com.akkaserverless.javasdk.reply.FailureReply;
import com.akkaserverless.javasdk.reply.MessageReply;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityEffect;
import com.example.CounterApi;
import com.google.protobuf.Empty;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;

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

        CommandContext<CounterDomain.CounterState> context = Mockito.mock(CommandContext.class); // <2>
        Optional<CounterDomain.CounterState> currentState = Optional.empty();
        ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder =
                ValueEntityEffect.newBuilder(Empty.class, CounterDomain.CounterState.class);

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(42).build(); // <3>
        entity.increase(message, currentState, effectBuilder, context); // <4>

        // FIXME
        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(42).build()); // <5>
    }
    // end::increase[]
    @Test
    public void increaseWithPriorState() {
        entity = new CounterImpl(entityId);

        CommandContext<CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Optional<CounterDomain.CounterState> currentState = Optional.of(CounterDomain.CounterState.newBuilder().setValue(13).build());
        ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder =
                ValueEntityEffect.newBuilder(Empty.class, CounterDomain.CounterState.class);

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(42).build();
        entity.increase(message, currentState, effectBuilder, context);

        // FIXME
        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(13 + 42).build());
    }

    @Test
    public void increaseShouldFailWithNegativeValue() {
        entity = new CounterImpl(entityId);

        CommandContext<CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Optional<CounterDomain.CounterState> currentState = Optional.of(CounterDomain.CounterState.newBuilder().setValue(27).build());
        ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder =
                ValueEntityEffect.newBuilder(Empty.class, CounterDomain.CounterState.class);

        CounterApi.IncreaseValue message = CounterApi.IncreaseValue.newBuilder().setValue(-2).build();
        Effect<Empty> reply = entity.increase(message, currentState, effectBuilder, context);
        assertThat(reply,  is(instanceOf(FailureReply.class)));
    }

    @Test
    public void decreaseNoPriorState() {
        entity = new CounterImpl(entityId);

        CommandContext<CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Optional<CounterDomain.CounterState> currentState = Optional.empty();
        ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder =
                ValueEntityEffect.newBuilder(Empty.class, CounterDomain.CounterState.class);

        CounterApi.DecreaseValue message = CounterApi.DecreaseValue.newBuilder().setValue(42).build();
        entity.decrease(message, currentState, effectBuilder, context);

        // FIXME
        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(-42).build());
    }
    
    @Test
    public void resetTest() {
        entity = new CounterImpl(entityId);

        CommandContext<CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Optional<CounterDomain.CounterState> currentState = Optional.of(CounterDomain.CounterState.newBuilder().setValue(13).build());
        ValueEntityEffect.Builder<Empty, CounterDomain.CounterState> effectBuilder =
                ValueEntityEffect.newBuilder(Empty.class, CounterDomain.CounterState.class);

        CounterApi.ResetValue message = CounterApi.ResetValue.newBuilder().build();
        entity.reset(message, currentState, effectBuilder, context);

        // FIXME
        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(0).build());
    }
    
    @Test
    public void getCurrentCounterTest() {
        entity = new CounterImpl(entityId);

        CommandContext<CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Optional<CounterDomain.CounterState> currentState = Optional.of(CounterDomain.CounterState.newBuilder().setValue(13).build());
        ValueEntityEffect.Builder<CounterApi.CurrentCounter, CounterDomain.CounterState> effectBuilder =
                ValueEntityEffect.newBuilder(CounterApi.CurrentCounter.class, CounterDomain.CounterState.class);

        CounterApi.GetCounter message = CounterApi.GetCounter.newBuilder().build();
        Effect<CounterApi.CurrentCounter> reply = entity.getCurrentCounter(message, currentState, effectBuilder, context);

        assertThat(((MessageReply<CounterApi.CurrentCounter>) reply).payload().getValue(), is(13));
    }

}
