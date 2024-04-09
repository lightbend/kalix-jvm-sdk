/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
