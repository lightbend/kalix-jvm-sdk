/* This code was initialised by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.domain;

import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.example.CounterApi;
import com.google.protobuf.Empty;
import org.junit.Test;
import org.mockito.*;

import static org.junit.Assert.assertThrows;

public class CounterTest {
    private String entityId = "entityId1";
    private Counter entity;
    private CommandContext<CounterDomain.CounterState> context = Mockito.mock(CommandContext.class);
    
    @Test
    public void increaseTest() {
        entity = new Counter(entityId);
        
        // TODO: write your mock here
        // Mockito.when(context.[...]).thenReturn([...]);
        
        // TODO: set fields in command, and update assertions to verify implementation
        // assertEquals([expected],
        //    entity.increase(CounterApi.IncreaseValue.newBuilder().build(), context);
        // );
    }
    
    @Test
    public void decreaseTest() {
        entity = new Counter(entityId);
        
        // TODO: write your mock here
        // Mockito.when(context.[...]).thenReturn([...]);
        
        // TODO: set fields in command, and update assertions to verify implementation
        // assertEquals([expected],
        //    entity.decrease(CounterApi.DecreaseValue.newBuilder().build(), context);
        // );
    }
    
    @Test
    public void resetTest() {
        entity = new Counter(entityId);
        
        // TODO: write your mock here
        // Mockito.when(context.[...]).thenReturn([...]);
        
        // TODO: set fields in command, and update assertions to verify implementation
        // assertEquals([expected],
        //    entity.reset(CounterApi.ResetValue.newBuilder().build(), context);
        // );
    }
    
    @Test
    public void getCurrentCounterTest() {
        entity = new Counter(entityId);
        
        // TODO: write your mock here
        // Mockito.when(context.[...]).thenReturn([...]);
        
        // TODO: set fields in command, and update assertions to verify implementation
        // assertEquals([expected],
        //    entity.getCurrentCounter(CounterApi.GetCounter.newBuilder().build(), context);
        // );
    }
}