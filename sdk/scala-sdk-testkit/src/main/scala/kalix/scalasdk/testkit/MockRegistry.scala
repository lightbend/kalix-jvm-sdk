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

import kalix.scalasdk.testkit.impl.MockRegistryImpl

import scala.reflect.ClassTag

/**
 * This trait is meant to allow for unit testing when a service has cross-component or cross-service calls. The set of
 * mocks or stubs will be matched by its class type upon a call of an external component or service.
 */
trait MockRegistry {

  /**
   * Returns a new MockRegistry with the new mock added to previous ones.
   *
   * @param instance
   *   The instance object to be used as a mock.
   * @param expectedClass
   *   The class type of the object being mocked.
   * @tparam T
   *   The service interface to be mocked.
   * @return
   *   A copy of this MockRegistry.
   */
  def withMock[T](instance: T)(implicit expectedClass: ClassTag[T]): MockRegistry

  /**
   * Retrieves the existing mock for a given class type.
   *
   * @param clazz
   *   The class type to match on the set of mocks.
   * @tparam T
   *   The service interface to be mocked.
   * @return
   *   An Optional containing the existing mock for the given class type or None otherwise.
   */
  def get[T](clazz: Class[T]): Option[T]
}

object MockRegistry {
  val empty = new MockRegistryImpl()

  /**
   * Returns an instance of MockRegistry populated with the given set of mocks
   *
   * @param mocks
   *   the set of instances to serve as mocks
   * @return
   *   a new instance of MockRegistry
   */
  def of(mocks: Map[Class[_], Any]): MockRegistry = new MockRegistryImpl(mocks)

  /**
   * Returns a new MockRegistry with the new mock added to previous ones.
   *
   * @param instance
   *   The instance object to be used as a mock.
   * @param expectedClass
   *   The class type of the object being mocked.
   * @tparam T
   *   The service interface to be mocked.
   * @return
   *   A copy of this MockRegistry.
   */
  def withMock[T](instance: T)(implicit expectedClass: ClassTag[T]): MockRegistry =
    new MockRegistryImpl(Map(expectedClass.runtimeClass -> instance))
}
