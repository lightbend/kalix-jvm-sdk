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

package com.akkaserverless.javasdk.tck.model.localpersistenceeventing;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.MessageEnvelope;
import com.akkaserverless.javasdk.impl.action.ActionHandler;

public class LocalPersistenceSubscriberHandler extends ActionHandler<LocalPersistenceSubscriber> {

  public LocalPersistenceSubscriberHandler(LocalPersistenceSubscriber actionBehavior) {
    super(actionBehavior);
  }

  @Override
  public Action.Effect<?> handleUnary(String commandName, MessageEnvelope<Object> message) {
    switch (commandName) {
      case "ProcessEventOne":
        throw new RuntimeException("not implemented");

      case "ProcessAnyEvent":
        throw new RuntimeException("not implemented");

      case "ProcessValueOne":
        throw new RuntimeException("not implemented");

      case "ProcessAnyValue":
        throw new RuntimeException("not implemented");

      case "Effect":
        throw new RuntimeException("not implemented");

      default:
        throw new ActionHandler.HandlerNotFound(commandName);
    }
  }

  @Override
  public Source<Action.Effect<?>, NotUsed> handleStreamedOut(
      String commandName, MessageEnvelope<Object> message) {
    switch (commandName) {
      case "ProcessEventTwo":
        throw new RuntimeException("not implemented");

      case "ProcessValueTwo":
        throw new RuntimeException("not implemented");

      default:
        throw new ActionHandler.HandlerNotFound(commandName);
    }
  }

  @Override
  public Action.Effect<?> handleStreamedIn(
      String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
    throw new ActionHandler.HandlerNotFound(commandName);
  }

  @Override
  public Source<Action.Effect<?>, NotUsed> handleStreamed(
      String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
    throw new ActionHandler.HandlerNotFound(commandName);
  }
}
