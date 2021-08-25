/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akkaserverless.javasdk.tck.model.action;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.action.MessageEnvelope;
import com.akkaserverless.javasdk.impl.action.ActionHandler;
import com.akkaserverless.tck.model.Action;

import java.util.concurrent.CompletionStage;

public class ActionTckModelBehaviorHandler extends ActionHandler<ActionTckModelBehavior> {

  public ActionTckModelBehaviorHandler(ActionTckModelBehavior actionBehavior) {
    super(actionBehavior);
  }

  @Override
  public CompletionStage<Reply<Object>> handleUnary(
      String commandName, MessageEnvelope<Object> message) throws Throwable {
    switch (commandName) {
      case "ProcessUnary":
        return action()
            .processUnary((Action.Request) message.payload())
            .thenApply(Reply::mapToObject);

      default:
        throw new ActionHandler.HandlerNotFound(commandName);
    }
  }

  @Override
  public Source<Reply<Object>, NotUsed> handleStreamedOut(
      String commandName, MessageEnvelope<Object> message) {
    switch (commandName) {
      case "ProcessStreamedOut":
        return action()
            .processStreamedOut((Action.Request) message.payload())
            .map(Reply::mapToObject);

      default:
        throw new ActionHandler.HandlerNotFound(commandName);
    }
  }

  @Override
  public CompletionStage<Reply<Object>> handleStreamedIn(
      String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
    switch (commandName) {
      case "ProcessStreamedIn":
        return action()
            .processStreamedIn(stream.map(el -> (Action.Request) el.payload()))
            .thenApply(Reply::mapToObject);

      default:
        throw new ActionHandler.HandlerNotFound(commandName);
    }
  }

  @Override
  public Source<Reply<Object>, NotUsed> handleStreamed(
      String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
    switch (commandName) {
      case "ProcessStreamed":
        return action()
            .processStreamed(stream.map(el -> (Action.Request) el.payload()))
            .map(Reply::mapToObject);

      default:
        throw new ActionHandler.HandlerNotFound(commandName);
    }
  }
}
