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

package kalix.scalasdk.testkit

import kalix.scalasdk.testkit.impl.TestKitMockRegistryImpl

/**
 * This trait is meant to allow for unit testing when a service has cross-component or cross-service calls. The set of
 * mocks or stubs will be matched by its class type upon a call of an external component or service.
 */
trait TestKitMockRegistry {

  /**
   * Return a new TestKitMockRegistry with the new mock added to previous ones.
   *
   * @param instance
   *   the instance object to be used as a mock
   * @tparam T
   * @return
   *   A copy of this TestKitMockRegistry.
   */
  def withMock[T](instance: T): TestKitMockRegistry

  /**
   * Retrieve the existing mock for a given class type.
   *
   * @param clazz
   *   the class type to match on the set of mocks
   * @tparam T
   * @return
   *   an Optional containing the existing mock for the given class type or None otherwise.
   */
  def get[T](clazz: Class[T]): Option[T]
}

object TestKitMockRegistry {
  val empty = new TestKitMockRegistryImpl()

  def of(set: Set[Any]): TestKitMockRegistry = new TestKitMockRegistryImpl(set)

  def withMock[T](instance: T): TestKitMockRegistry = new TestKitMockRegistryImpl(Set(instance))
}
