/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
}

object MockRegistry {
  val empty = new MockRegistryImpl()

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
