/*
 * Copyright 2024 Lightbend Inc.
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
