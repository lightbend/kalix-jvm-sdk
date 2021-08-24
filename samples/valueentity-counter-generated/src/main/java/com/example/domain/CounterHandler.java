/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */
package com.example.domain;

import com.akkaserverless.javasdk.impl.AnySupport;
import com.akkaserverless.javasdk.impl.EntityExceptions;
import com.akkaserverless.javasdk.impl.valueentity.AdaptedCommandContextWithState;
import com.akkaserverless.javasdk.lowlevel.ValueEntityHandler;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityBase;
import com.example.CounterApi;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import java.util.Optional;
import scalapb.UnknownFieldSet;

/** A value entity handler */
public class CounterHandler implements ValueEntityHandler {

  public static final Descriptors.ServiceDescriptor serviceDescriptor =
      CounterApi.getDescriptor().findServiceByName("CounterService");
  public static final String entityType = "counter";

  private final Counter entity;
  
  public CounterHandler(Counter entity) {
    this.entity = entity;
  }

  @Override
  public ValueEntityBase.Effect<? extends GeneratedMessageV3> handleCommand(
      Any command, Any state, CommandContext<Any> context) throws Throwable {
      
    CounterDomain.CounterState parsedState =
      CounterDomain.CounterState.parseFrom(state.getValue());

    CommandContext<CounterDomain.CounterState> adaptedContext =
        new AdaptedCommandContextWithState(context, parsedState);

    entity.setCommandContext(Optional.of(adaptedContext));
    
    try {
      switch (context.commandName()) {

        case "Increase":
          return entity.increase(
              parsedState,
              CounterApi.IncreaseValue.parseFrom(command.getValue()));

        case "Decrease":
          return entity.decrease(
              parsedState,
              CounterApi.DecreaseValue.parseFrom(command.getValue()));

        case "Reset":
          return entity.reset(
              parsedState,
              CounterApi.ResetValue.parseFrom(command.getValue()));

        case "GetCurrentCounter":
          return entity.getCurrentCounter(
              parsedState,
              CounterApi.GetCounter.parseFrom(command.getValue()));

        default:
          throw new EntityExceptions.EntityException(
              context.entityId(),
              context.commandId(),
              context.commandName(),
              "No command handler found for command ["
                  + context.commandName()
                  + "] on "
                  + entity.getClass());
      }
    } finally {
      entity.setCommandContext(Optional.empty());
    }
  }
  
  @Override
  public com.google.protobuf.any.Any emptyState() {
    return com.google.protobuf.any.Any.apply(
        AnySupport.DefaultTypeUrlPrefix()
          + "/"
          + CounterDomain.CounterState.getDescriptor().getFullName(),
        entity.emptyState().toByteString(),
        UnknownFieldSet.empty());
  }
}