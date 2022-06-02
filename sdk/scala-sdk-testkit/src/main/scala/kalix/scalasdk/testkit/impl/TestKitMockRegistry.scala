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

package kalix.scalasdk.testkit.impl

/**
 * This class is meant to hold mocks used in unit testing cross-component calls
 * @param mocks
 *   set of mocks or stubs that will be matched by the class upon an external call within a component
 */
final class TestKitMockRegistry(mocks: Set[Any]) {
  def get[T](key: Class[T]): Option[T] = mocks.collectFirst {
    case m if m.getClass.getAnnotatedInterfaces.exists(_.getType == key) => key.cast(m)
  }
}

object TestKitMockRegistry {
  val empty = new TestKitMockRegistry(Set.empty)
}
