package com.example.replicated.multimap;

import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityRouter;
import com.akkaserverless.javasdk.replicatedentity.CommandContext;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedMultiMap;
import com.google.protobuf.Empty;

// This code is managed by Akka Serverless tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * A replicated entity handler that is the glue between the Protobuf service <code>MultiMapService</code>
 * and the command handler methods in the <code>SomeMultiMap</code> class.
 */
public class SomeMultiMapRouter extends ReplicatedEntityRouter<ReplicatedMultiMap<SomeMultiMapDomain.SomeKey, SomeMultiMapDomain.SomeValue>, SomeMultiMap> {

  public SomeMultiMapRouter(SomeMultiMap entity) {
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
