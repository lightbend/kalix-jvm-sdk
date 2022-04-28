package com.example.replicated.multimap;

import com.example.replicated.multimap.domain.SomeMultiMapDomain;
import com.google.protobuf.Empty;
import kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter;
import kalix.javasdk.replicatedentity.CommandContext;
import kalix.javasdk.replicatedentity.ReplicatedEntity;
import kalix.javasdk.replicatedentity.ReplicatedMultiMap;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A replicated entity handler that is the glue between the Protobuf service <code>MultiMapService</code>
 * and the command handler methods in the <code>MultiMapServiceEntity</code> class.
 */
public class MultiMapServiceEntityRouter extends ReplicatedEntityRouter<ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue>, MultiMapServiceEntity> {

  public MultiMapServiceEntityRouter(MultiMapServiceEntity entity) {
    super(entity);
  }

  @Override
  public ReplicatedEntity.Effect<?> handleCommand(
      String commandName, ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue> data, Object command, CommandContext context) {
    switch (commandName) {

      case "Put":
        return entity().put(data, (SomeMultiMapApi.PutValue) command);

      default:
        throw new ReplicatedEntityRouter.CommandHandlerNotFound(commandName);
    }
  }
}
