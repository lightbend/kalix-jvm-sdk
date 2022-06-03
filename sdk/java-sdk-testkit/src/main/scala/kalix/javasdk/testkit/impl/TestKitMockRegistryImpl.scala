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

package kalix.javasdk.testkit.impl

import kalix.javasdk.testkit.TestKitMockRegistry

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters.RichOption

/**
 * This class is meant to hold mocks used in unit testing cross-component calls
 */
private[kalix] class TestKitMockRegistryImpl(var mocks: Map[Class[_], Any]) extends TestKitMockRegistry {

  def this(mocks: java.util.Map[Class[_], Any]) {
    this(mocks.asScala.toMap)
  }

  override def get[T](key: Class[T]): java.util.Optional[T] =
    mocks
      .get(key)
      .map(key.cast)
      .toJava

  override def addMock[T](clazz: Class[T], instance: T): TestKitMockRegistry = {
    mocks = mocks + (clazz -> instance)
    this
  }
}

object TestKitMockRegistryImpl {
  val empty = new TestKitMockRegistryImpl(Map.empty[Class[_], Any])
}
