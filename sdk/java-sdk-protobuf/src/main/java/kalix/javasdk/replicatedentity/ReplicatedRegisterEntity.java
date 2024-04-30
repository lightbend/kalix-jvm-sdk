/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

public abstract class ReplicatedRegisterEntity<T> extends ReplicatedEntity<ReplicatedRegister<T>> {
  /**
   * Implement to set the default empty value for the register.
   *
   * @return the default empty value
   */
  public abstract T emptyValue();

  @Override
  public final ReplicatedRegister<T> emptyData(ReplicatedDataFactory factory) {
    return factory.newRegister(emptyValue());
  }
}
