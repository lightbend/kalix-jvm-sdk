package com.example.domain;

import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.example.CounterApi;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;

public class CounterTest {
    private final String entityId = "entityId1";
    private CounterImpl entity;

    private static class MockedContextFailure extends RuntimeException {};

    @Test
    public void increaseNoPriorState() {
        entity = new CounterImpl(entityId);
        CommandContext<CounterDomain.CounterState> context = contextWithoutState();

        CounterApi.IncreaseValue command = CounterApi.IncreaseValue.newBuilder().setValue(42).build();
        entity.increaseWithReply(command, context);

        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(42).build());
    }

    @Test
    public void increaseWithPriorState() {
        entity = new CounterImpl(entityId);
        CommandContext<CounterDomain.CounterState> context = getCounterStateCommandContext(13);

        CounterApi.IncreaseValue command = CounterApi.IncreaseValue.newBuilder().setValue(42).build();
        entity.increaseWithReply(command, context);

        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(13 + 42).build());
    }

    @Test
    public void increaseShouldFailWithNegativeValue() {
        entity = new CounterImpl(entityId);
        CommandContext<CounterDomain.CounterState> context = getCounterStateCommandContext(27);
        Mockito.when(context.fail(anyString()))
                .thenReturn(new MockedContextFailure());

        assertThrows(MockedContextFailure.class, () -> {
            CounterApi.IncreaseValue command = CounterApi.IncreaseValue.newBuilder().setValue(-2).build();
            entity.increaseWithReply(command, context);
        });
    }

    @Test
    public void decreaseNoPriorState() {
        entity = new CounterImpl(entityId);
        CommandContext<CounterDomain.CounterState> context = contextWithoutState();

        CounterApi.DecreaseValue command = CounterApi.DecreaseValue.newBuilder().setValue(42).build();
        entity.decreaseWithReply(command, context);

        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(-42).build());
    }
    
    @Test
    public void resetTest() {
        entity = new CounterImpl(entityId);
        CommandContext<CounterDomain.CounterState> context = getCounterStateCommandContext(13);

        CounterApi.ResetValue command = CounterApi.ResetValue.newBuilder().build();
        entity.reset(command, context);

        Mockito.verify(context).updateState(CounterDomain.CounterState.newBuilder().setValue(0).build());
    }
    
    @Test
    public void getCurrentCounterTest() {
        entity = new CounterImpl(entityId);

        CommandContext<CounterDomain.CounterState> context = getCounterStateCommandContext(13);

        CounterApi.GetCounter command = CounterApi.GetCounter.newBuilder().build();
        CounterApi.CurrentCounter reply = entity.getCurrentCounter(command, context);

        assertThat(reply.getValue(), is(13));
    }

    private CommandContext<CounterDomain.CounterState> contextWithoutState() {
        CommandContext<CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Mockito.when(context.getState()).thenReturn(Optional.empty());
        return context;
    }

    private CommandContext<CounterDomain.CounterState> getCounterStateCommandContext(int value) {
        CommandContext<CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
        Mockito.when(context.getState()).thenReturn(Optional.of(CounterDomain.CounterState.newBuilder().setValue(value).build()));
        return context;
    }

}
