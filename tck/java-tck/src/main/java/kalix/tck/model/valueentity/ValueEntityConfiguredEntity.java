/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.tck.model.valueentity;

import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.tck.model.valueentity.ValueEntityApi.*;

public class ValueEntityConfiguredEntity extends AbstractValueEntityConfiguredEntity {

  public ValueEntityConfiguredEntity(ValueEntityContext context) {}

  public Effect<Response> call(Persisted state, Request request) {
    return effects().reply(Response.getDefaultInstance());
  }

  @Override
  public Persisted emptyState() {
    return null;
  }
}
