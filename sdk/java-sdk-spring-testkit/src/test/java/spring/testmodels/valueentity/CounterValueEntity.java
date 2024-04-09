/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testmodels.valueentity;

import kalix.javasdk.valueentity.ValueEntity;

public class CounterValueEntity extends ValueEntity<Integer> {

  @Override
  public Integer emptyState() {
    return 0;
  }

  public Effect<String> increaseBy(Integer value) {
    Integer state = currentState();
    if (value < 0) return effects().error("Can't increase with a negative value");
    else return effects().updateState(state + value).thenReply("Ok");
  }

  public Effect<String> increaseFromMeta() {
    Integer state = currentState();
    return effects().updateState(state + Integer.parseInt(commandContext().metadata().get("value").get())).thenReply("Ok");
  }

  public Effect<String> delete() {
    return effects().deleteEntity().thenReply("Deleted");
  }
}
