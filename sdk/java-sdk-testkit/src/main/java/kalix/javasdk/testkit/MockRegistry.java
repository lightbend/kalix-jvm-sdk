/*
 * Copyright 2021 Lightbend Inc.
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

package kalix.javasdk.testkit;

import kalix.javasdk.testkit.impl.MockRegistryImpl;

import java.util.Map;
import java.util.Optional;

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
  <T> MockRegistry addMock(Class<T> clazz, T instance);

  /**
   * Retrieves the existing mock for a given class type.
   *
   * @param clazz The class type to match on the set of mocks.
   * @return An Optional containing the existing mock for the given class type or None otherwise.
   * @param <T> The service interface to be mocked.
   */
  <T> Optional<T> get(Class<T> clazz);

  /**
   * Returns an instance of MockRegistry populated with the given set of mocks
   *
   * @param mocks the set of instances to serve as mocks
   * @return a new instance of MockRegistry
   */
  static MockRegistry of(Map<Class<?>, Object> mocks) {
    return new MockRegistryImpl(mocks);
  }

  /**
   * Returns an instance of MockRegistry populated with the mock provided
   *
   * @param clazz The class type used to identify the mock.
   * @param instance The instance that will be used as a mock.
   * @return A new instance of MockRegistry.
   * @param <T> The service interface to be mocked.
   */
  static <T> MockRegistry withMock(Class<T> clazz, T instance) {
    return new MockRegistryImpl(Map.of(clazz, instance));
  }

  MockRegistry EMPTY = MockRegistryImpl.empty();
}
