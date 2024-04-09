/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.valueentity;

import kalix.javasdk.PassivationStrategy;
import kalix.javasdk.impl.valueentity.ValueEntityOptionsImpl;

import java.util.Set;

/** Root entity options for all value based entities. */
public interface ValueEntityOptions extends kalix.javasdk.EntityOptions {

  @Override
  ValueEntityOptions withForwardHeaders(Set<String> headers);

  /**
   * Create a default entity option for a value based entity.
   *
   * @return the entity option
   */
  static ValueEntityOptions defaults() {
    return ValueEntityOptionsImpl.defaults();
  }
}
