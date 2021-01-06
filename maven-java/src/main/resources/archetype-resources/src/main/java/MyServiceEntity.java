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
  public Myentity.MyState snapshot() {
    return Myentity.MyState.newBuilder().setValue(this.value).build();
  }

  @SnapshotHandler
  public void handleSnapshot(Myentity.MyState state) {
    this.value = state.getValue();
  }

  @EventHandler
  public void valueSet(Domain.ValueSet event) {
    this.value = event.getValue();
  }

  @CommandHandler
  public Empty set(Myentity.SetValue setValue, CommandContext commandContext) {
    this.value = setValue.getValue();

    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Myentity.MyState get(Myentity.GetValue getValue, CommandContext commandContext) {
    return Myentity.MyState.newBuilder().setValue(value).build();
  }
}
