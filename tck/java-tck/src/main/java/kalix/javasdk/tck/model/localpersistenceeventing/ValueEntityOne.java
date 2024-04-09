/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck.model.localpersistenceeventing;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.tck.model.eventing.LocalPersistenceEventing;
import com.google.protobuf.Empty;

public class ValueEntityOne extends ValueEntity<Object> {
  public ValueEntityOne(ValueEntityContext context) {}

  public Effect<Empty> updateValue(
      Object state, LocalPersistenceEventing.UpdateValueRequest value) {
    if (value.hasValueOne()) {
      return effects().updateState(value.getValueOne()).thenReply(Empty.getDefaultInstance());
    } else {
      return effects().updateState(value.getValueTwo()).thenReply(Empty.getDefaultInstance());
    }
  }

  @Override
  public Object emptyState() {
    return null;
  }
}
