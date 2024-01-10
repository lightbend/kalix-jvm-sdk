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

import kalix.javasdk.impl.valueentity.ValueEntityRouter;
import kalix.javasdk.valueentity.CommandContext;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.tck.model.eventing.LocalPersistenceEventing;

/** A value entity handler */
public class ValueEntityTwoRouter extends ValueEntityRouter<Object, ValueEntityTwo> {

  public ValueEntityTwoRouter(ValueEntityTwo entity) {
    super(entity);
  }

  @Override
  public ValueEntity.Effect<?> handleCommand(
      String commandName, Object state, Object command, CommandContext context) {
    switch (commandName) {
      case "UpdateJsonValue":
        return entity().updateJsonValue(state, (LocalPersistenceEventing.JsonValue) command);
      default:
        throw new CommandHandlerNotFound(commandName);
    }
  }
}
