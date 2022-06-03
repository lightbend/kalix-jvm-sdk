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

import kalix.javasdk.testkit.impl.TestKitMockRegistryImpl;

import java.util.Map;
import java.util.Optional;

/**
 * This trait is meant to allow for unit testing when a service has cross-component or cross-service calls. The set of
 * mocks or stubs will be matched by its class type upon a call of an external component or service.
 */
public interface TestKitMockRegistry {
  /**
   * Returns a new TestKitMockRegistry with the new mock added to previous ones.
   *
   * @param instance The instance object to be used as a mock
   * @return A copy of this TestKitMockRegistry.
   * @param <T>
   */
  <T> TestKitMockRegistry addMock(Class<T> clazz, T instance);

  /**
   * Retrieves the existing mock for a given class type.
   *
   * @param clazz The class type to match on the set of mocks.
   * @return An Optional containing the existing mock for the given class type or None otherwise.
   * @param <T>
   */
  <T> Optional<T> get(Class<T> clazz);

  /**
   * Returns an instance of TestKitMockRegistry populated with the given set of mocks
   *
   * @param mocks the set of instances to serve as mocks
   * @return a new instance of TestKitMockRegistry
   */
  static TestKitMockRegistry of(Map<Class<?>, Object> mocks) {
    return new TestKitMockRegistryImpl(mocks);
  }

  /**
   * Returns an instance of TestKitMockRegistry populated with the mock provided
   *
   * @param clazz the class type used to identify the mock
   * @param instance the instance that will be used as a mock
   * @return a new instance of TestKitMockRegistry
   * @param <T>
   */
  static <T> TestKitMockRegistry withMock(Class<T> clazz, T instance) {
    return new TestKitMockRegistryImpl(Map.of(clazz, instance));
  }

  TestKitMockRegistry EMPTY = TestKitMockRegistryImpl.empty();
}
