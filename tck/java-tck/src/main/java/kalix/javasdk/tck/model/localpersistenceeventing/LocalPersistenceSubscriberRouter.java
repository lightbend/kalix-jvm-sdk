/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.MessageEnvelope;
import kalix.javasdk.impl.action.ActionRouter;
import kalix.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Any;

public class LocalPersistenceSubscriberRouter extends ActionRouter<LocalPersistenceSubscriber> {

  public LocalPersistenceSubscriberRouter(LocalPersistenceSubscriber actionBehavior) {
    super(actionBehavior);
  }

  @Override
  public Action.Effect<?> handleUnary(String commandName, MessageEnvelope<Object> message) {
    switch (commandName) {
      case "ProcessEventOne":
        return action().processEventOne((LocalPersistenceEventing.EventOne) message.payload());

      case "ProcessAnyEvent":
        return action().processAnyEvent((Any) message.payload());

      case "ProcessValueOne":
        return action().processValueOne((LocalPersistenceEventing.ValueOne) message.payload());

      case "ProcessAnyValue":
        return action().processAnyValue((Any) message.payload());

      case "Effect":
        return action().effect((LocalPersistenceEventing.EffectRequest) message.payload());

      default:
        throw new ActionRouter.HandlerNotFound(commandName);
    }
  }

  @Override
  public Source<Action.Effect<?>, NotUsed> handleStreamedOut(
      String commandName, MessageEnvelope<Object> message) {
    switch (commandName) {
      case "ProcessEventTwo":
        return (Source<Action.Effect<?>, NotUsed>)
            (Object)
                action().processEventTwo((LocalPersistenceEventing.EventTwo) message.payload());

      case "ProcessValueTwo":
        return (Source<Action.Effect<?>, NotUsed>)
            (Object)
                action().processValueTwo((LocalPersistenceEventing.ValueTwo) message.payload());

      default:
        throw new ActionRouter.HandlerNotFound(commandName);
    }
  }

  @Override
  public Action.Effect<?> handleStreamedIn(
      String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
    throw new ActionRouter.HandlerNotFound(commandName);
  }

  @Override
  public Source<Action.Effect<?>, NotUsed> handleStreamed(
      String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
    throw new ActionRouter.HandlerNotFound(commandName);
  }
}
