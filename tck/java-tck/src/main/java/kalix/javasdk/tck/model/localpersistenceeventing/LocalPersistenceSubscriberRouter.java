/*
 * Copyright 2024 Lightbend Inc.
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
