package $package;

import ${package}.persistence.Domain;
import com.google.protobuf.Empty;
import io.cloudstate.javasupport.EntityId;
import io.cloudstate.javasupport.eventsourced.*;

/** An event sourced entity. */
@EventSourcedEntity
public class MyServiceEntity {
  @SuppressWarnings("unused")
  private final String entityId;

  private int value = 0;

  public MyServiceEntity(@EntityId String entityId) {
    this.entityId = entityId;
  }

  /**
   * This method will be called when snapshot is created
   */
  @Snapshot
  public MyEntity.MyState snapshot() {
    return MyEntity.MyState.newBuilder().setValue(this.value).build();
  }

  @SnapshotHandler
  public void handleSnapshot(MyEntity.MyState state) {
    this.value = state.getValue();
  }

  /**
   * Handle the ValueSet event, updating the persisted value
   * @param event the event payload containing the updated value
   */
  @EventHandler
  public void valueSet(Domain.ValueSet event) {
    this.value = event.getValue();
  }

  /**
   * Handle the SetValue command
   * Dispatches a ValueSet event with the provided value.
   * @param command the command payload, containing the value to update to
   * @param commandContext the Akka Serverless command context
   */
  @CommandHandler
  public Empty setValue(MyEntity.SetValueCommand command, CommandContext commandContext) {
    commandContext.emit(Domain.ValueSet.newBuilder().setValue(command.getValue()).build());
    return Empty.getDefaultInstance();
  }

  /**
   * Handle the GetValue command
   * Returns a state payload with the persisted value.
   * @param command the command payload
   * @param commandContext the Akka Serverless command context
   * @return a MyState payload conatining the persisted value
   */
  @CommandHandler
  public MyEntity.MyState getValue(MyEntity.GetValueCommand command, CommandContext commandContext) {
    return MyEntity.MyState.newBuilder().setValue(this.value).build();
  }
}
