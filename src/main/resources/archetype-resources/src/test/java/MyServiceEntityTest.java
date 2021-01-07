package ${package};

import io.cloudstate.javasupport.eventsourced.CommandContext;
import org.testng.Assert;
import org.testng.annotations.*;
import org.mockito.*;
import ${package}.persistence.Domain.*;

import java.io.IOException;

public class MyServiceEntityTest {

    String entityId = "entityId1";
    MyServiceEntity entity;
    CommandContext context = Mockito.mock(CommandContext.class);

    @Test
    public void activateDeviceTest() {
        // Instantiate entity and send activation command.
        entity = new MyServiceEntity(entityId);
        ValueSet event = ValueSet.newBuilder().setValue(8).build();
        entity.setValue(MyEntity.SetValueCommand.newBuilder().setEntityId(entityId).setValue(8).build(), context);
        Mockito.verify(context).emit(event);

        // Simulate event callback.
        entity.valueSet(event);

        // Test get device.
        MyEntity.GetValueCommand command = MyEntity.GetValueCommand.newBuilder().setEntityId(entityId).build();
        MyEntity.MyState currentState = entity.getValue(command, context);
        Assert.assertEquals(currentState.getValue(), 8);
    }
}