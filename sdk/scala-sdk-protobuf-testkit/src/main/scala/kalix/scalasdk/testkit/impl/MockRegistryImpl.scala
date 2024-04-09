/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.testkit.impl

import kalix.scalasdk.testkit.MockRegistry

import scala.reflect.ClassTag

private[kalix] class MockRegistryImpl(var mocks: Map[Class[_], Any] = Map.empty) extends MockRegistry {

  override def withMock[T](instance: T)(implicit expectedClass: ClassTag[T]): MockRegistry = {
    mocks = mocks + (expectedClass.runtimeClass -> instance)
    this
  }

  def get[T](clazz: Class[T]): Option[T] = mocks
    .get(clazz)
    .map(clazz.cast)
}
