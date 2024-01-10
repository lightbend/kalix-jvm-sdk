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

package kalix.javasdk.tck.model.replicatedentity;

import kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter;
import kalix.javasdk.replicatedentity.CommandContext;
import kalix.replicatedentity.ReplicatedData;
import kalix.javasdk.replicatedentity.ReplicatedEntity;
import kalix.tck.model.ReplicatedEntity.Request;

public class ReplicatedEntityTckModelEntityRouter
    extends ReplicatedEntityRouter<ReplicatedData, ReplicatedEntityTckModelEntity> {

  public ReplicatedEntityTckModelEntityRouter(ReplicatedEntityTckModelEntity entity) {
    super(entity);
  }

  @Override
  public ReplicatedEntity.Effect<?> handleCommand(
      String commandName, ReplicatedData data, Object command, CommandContext context) {
    switch (commandName) {
      case "Process":
        return entity().process(data, (Request) command);
      default:
        throw new CommandHandlerNotFound(commandName);
    }
  }
}
