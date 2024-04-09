/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit;

import kalix.javasdk.testkit.impl.MockRegistryImpl;

/**
 * This trait is meant to allow for unit testing when a service has cross-component or cross-service
 * calls. The set of mocks or stubs will be matched by its class type upon a call of an external
 * component or service.
 */
public interface MockRegistry {
  /**
   * Returns a new MockRegistry with the new mock added to previous ones.
   *
   * @param clazz The class type used to identify the mock.
   * @param instance The instance object to be used as a mock-
   * @return A copy of this MockRegistry.
   * @param <T> The service interface to be mocked.
   */
  <T> MockRegistry withMock(Class<T> clazz, T instance);

  /**
   * Returns an empty instance of MockRegistry that can be chained with `withMock`
   *
   * @return a new instance of MockRegistry
   */
  static MockRegistry create() {
    return MockRegistryImpl.empty();
  }

  MockRegistry EMPTY = MockRegistryImpl.empty();
}
