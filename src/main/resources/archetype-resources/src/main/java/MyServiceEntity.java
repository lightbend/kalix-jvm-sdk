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

  @EventHandler
  public void valueSet(Domain.ValueSet event) {
    this.value = event.getValue();
  }

  @CommandHandler
  public Empty set(MyEntity.SetValue setValue, CommandContext commandContext) {
    this.value = setValue.getValue();

    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public MyEntity.MyState get(MyEntity.GetValue getValue, CommandContext commandContext) {
    return MyEntity.MyState.newBuilder().setValue(value).build();
  }
}
