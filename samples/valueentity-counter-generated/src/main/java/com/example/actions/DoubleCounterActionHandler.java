/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */

package com.example.actions;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.MessageEnvelope;
import com.akkaserverless.javasdk.impl.action.ActionHandler;
import com.example.CounterApi;
import com.google.protobuf.Empty;
import java.util.concurrent.CompletionStage;

public class DoubleCounterActionHandler extends ActionHandler<DoubleCounterAction> {

  public DoubleCounterActionHandler(DoubleCounterAction actionBehavior) {
    super(actionBehavior);
  }

  @Override
  public Action.Effect<?> handleUnary(String commandName, MessageEnvelope<Object> message) {
    switch (commandName) {
      case "Increase":
        return action()
                 .increase((CounterApi.IncreaseValue) message.payload());
      default:
        throw new ActionHandler.HandlerNotFound(commandName);
    }
  }

  @Override
  public Source<Action.Effect<?>, NotUsed> handleStreamedOut(String commandName, MessageEnvelope<Object> message) {
    switch (commandName) {
      
      default:
        throw new ActionHandler.HandlerNotFound(commandName);
    }
  }

  @Override
  public Action.Effect<?> handleStreamedIn(String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
    switch (commandName) {
      
      default:
        throw new ActionHandler.HandlerNotFound(commandName);
    }
  }

  @Override
  public Source<Action.Effect<?>, NotUsed> handleStreamed(String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
    switch (commandName) {
      
      default:
        throw new ActionHandler.HandlerNotFound(commandName);
    }
  }
}