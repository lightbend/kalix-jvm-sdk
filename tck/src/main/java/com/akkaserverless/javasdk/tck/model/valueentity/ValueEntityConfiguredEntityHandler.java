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

package com.akkaserverless.javasdk.tck.model.valueentity;

import com.akkaserverless.javasdk.impl.valueentity.ValueEntityHandler;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityBase;
import com.akkaserverless.tck.model.ValueEntity;

/** A value entity handler */
public class ValueEntityConfiguredEntityHandler
    extends ValueEntityHandler<String, ValueEntityConfiguredEntity> {

  public ValueEntityConfiguredEntityHandler(ValueEntityConfiguredEntity entity) {
    super(entity);
  }

  @Override
  public ValueEntityBase.Effect<?> handleCommand(
      String commandName, String state, Object command, CommandContext context) {
    switch (commandName) {
      case "Call":
        return entity().call(state, (ValueEntity.Request) command);
      default:
        throw new CommandHandlerNotFound(commandName);
    }
  }
}
