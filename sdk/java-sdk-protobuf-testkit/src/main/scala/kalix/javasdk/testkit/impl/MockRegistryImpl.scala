/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit.impl

import kalix.javasdk.testkit.MockRegistry
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOption

private[kalix] class MockRegistryImpl(var mocks: Map[Class[_], Any]) extends MockRegistry {

  def this(mocks: java.util.Map[Class[_], Any]) = {
    this(mocks.asScala.toMap)
  }

  override def withMock[T](clazz: Class[T], instance: T): MockRegistry = {
    mocks = mocks + (clazz -> instance)
    this
  }

  def get[T](clazz: Class[T]): java.util.Optional[T] =
    mocks
      .get(clazz)
      .map(clazz.cast)
      .toJava
}

object MockRegistryImpl {
  val empty = new MockRegistryImpl(Map.empty[Class[_], Any])
}
