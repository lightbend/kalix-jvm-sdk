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

import kalix.javasdk.replicatedentity.ReplicatedCounter;
import kalix.javasdk.replicatedentity.ReplicatedDataFactory;
import kalix.javasdk.replicatedentity.ReplicatedEntity;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;
import kalix.tck.model.ReplicatedEntity.Request;
import kalix.tck.model.ReplicatedEntity.RequestAction;
import kalix.tck.model.ReplicatedEntity.Response;

public class ReplicatedEntityTwoEntity extends ReplicatedEntity<ReplicatedCounter> {

  public ReplicatedEntityTwoEntity(ReplicatedEntityContext context) {}

  @Override
  public ReplicatedCounter emptyData(ReplicatedDataFactory factory) {
    return factory.newCounter();
  }

  public Effect<Response> call(ReplicatedCounter counter, Request request) {
    if (request.getActionsList().stream().anyMatch(RequestAction::hasDelete)) {
      return effects().delete().thenReply(Response.getDefaultInstance());
    } else {
      return effects().reply(Response.getDefaultInstance());
    }
  }
}
