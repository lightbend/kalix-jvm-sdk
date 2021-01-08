package ${package};

import io.cloudstate.javasupport.eventsourced.CommandContext;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.*;
import ${package}.persistence.Domain.*;

public class MyServiceEntityTest {

    String entityId = "entityId1";
    MyServiceEntity entity;
    CommandContext context = Mockito.mock(CommandContext.class);

    @Test
    public void setValueTest() {
        // Given some arbitrary value and an instantiated entity
        int arbitraryValue = 8;
        entity = new MyServiceEntity(entityId);

        // When the SetValue command is issued with that value
        entity.setValue(MyEntity.SetValueCommand.newBuilder().setEntityId(entityId).setValue(arbitraryValue).build(), context);

        // Then a ValueSet event is emitted with that value
        ValueSet event = ValueSet.newBuilder().setValue(arbitraryValue).build();
        Mockito.verify(context).emit(event);

        // When that event is handled by the entity (with no dedicated testing harness we must manually provide the event to the handler)
        entity.valueSet(event);

        // And the current state is requested
        MyEntity.GetValueCommand command = MyEntity.GetValueCommand.newBuilder().setEntityId(entityId).build();
        MyEntity.MyState currentState = entity.getValue(command, context);

        // Then the result contains the updated value
        Assert.assertEquals(currentState.getValue(), arbitraryValue);
    }
}
