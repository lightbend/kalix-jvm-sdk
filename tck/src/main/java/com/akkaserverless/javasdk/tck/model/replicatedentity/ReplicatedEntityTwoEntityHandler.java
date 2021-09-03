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

package com.akkaserverless.javasdk.tck.model.replicatedentity;

import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityHandler;
import com.akkaserverless.javasdk.replicatedentity.CommandContext;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedCounter;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
import com.akkaserverless.tck.model.ReplicatedEntity.Request;

public class ReplicatedEntityTwoEntityHandler
    extends ReplicatedEntityHandler<ReplicatedCounter, ReplicatedEntityTwoEntity> {

  public ReplicatedEntityTwoEntityHandler(ReplicatedEntityTwoEntity entity) {
    super(entity);
  }

  @Override
  public ReplicatedEntity.Effect<?> handleCommand(
      String commandName, ReplicatedCounter data, Object command, CommandContext context) {
    switch (commandName) {
      case "Call":
        return entity().call(data, (Request) command);
      default:
        throw new CommandHandlerNotFound(commandName);
    }
  }
}
