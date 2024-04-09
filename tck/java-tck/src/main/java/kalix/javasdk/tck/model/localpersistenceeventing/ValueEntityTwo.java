/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.JsonSupport;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Empty;

public class ValueEntityTwo extends ValueEntity<Object> {
  public ValueEntityTwo(ValueEntityContext context) {}

  public Effect<Empty> updateJsonValue(Object state, LocalPersistenceEventing.JsonValue value) {
    return effects()
        // FIXME requirement to use JSON state should be removed from TCK
        .updateState(JsonSupport.encodeJson(new JsonMessage(value.getMessage())))
        .thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Object emptyState() {
    return null;
  }
}
