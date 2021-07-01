package com.example.domain;

import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.example.CounterApi;
import com.google.protobuf.Empty;
import org.junit.Test;
import org.mockito.*;

import static org.junit.Assert.assertThrows;

public class CounterTest {
    private String entityId = "entityId1";
    private CounterImpl entity;
    private CommandContext<CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
    
    private class MockedContextFailure extends RuntimeException {};
    
    @Test
    public void increaseTest() {
        entity = new CounterImpl(entityId);
        
        Mockito.when(context.fail("The command handler for `Increase` is not implemented, yet"))
            .thenReturn(new MockedContextFailure());
        
        // TODO: set fields in command, and update assertions to match implementation
        assertThrows(MockedContextFailure.class, () -> {
            entity.increaseWithReply(CounterApi.IncreaseValue.newBuilder().build(), context);
        });
    }
    
    @Test
    public void decreaseTest() {
        entity = new CounterImpl(entityId);
        
        Mockito.when(context.fail("The command handler for `Decrease` is not implemented, yet"))
            .thenReturn(new MockedContextFailure());
        
        // TODO: set fields in command, and update assertions to match implementation
        assertThrows(MockedContextFailure.class, () -> {
            entity.decreaseWithReply(CounterApi.DecreaseValue.newBuilder().build(), context);
        });
    }
    
    @Test
    public void resetTest() {
        entity = new CounterImpl(entityId);
        
        Mockito.when(context.fail("The command handler for `Reset` is not implemented, yet"))
            .thenReturn(new MockedContextFailure());
        
        // TODO: set fields in command, and update assertions to match implementation
        assertThrows(MockedContextFailure.class, () -> {
            entity.resetWithReply(CounterApi.ResetValue.newBuilder().build(), context);
        });
    }
    
    @Test
    public void getCurrentCounterTest() {
        entity = new CounterImpl(entityId);
        
        Mockito.when(context.fail("The command handler for `GetCurrentCounter` is not implemented, yet"))
            .thenReturn(new MockedContextFailure());
        
        // TODO: set fields in command, and update assertions to match implementation
        assertThrows(MockedContextFailure.class, () -> {
            entity.getCurrentCounterWithReply(CounterApi.GetCounter.newBuilder().build(), context);
        });
    }
}