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

package kalix.javasdk.impl.reflection

import kalix.javasdk.client.ComponentClientImpl
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class SomeClass {
  def a(): Unit = {}
  def b(): Unit = {}
  def c(): Unit = {}
  def c(p1: Int): Unit = {}
  def c(p1: String): Unit = {}
  def c(p1: String, p2: Int): Unit = {}
  def c(p1: Int, p2: Int): Unit = {}
}

class ReflectSpec extends AnyWordSpec with Matchers {

  "The reflection utils" must {
    "deterministically sort methods of the same class" in {
      import kalix.javasdk.impl.reflection.Reflect.methodOrdering
      val methods =
        classOf[SomeClass].getDeclaredMethods.toList.sorted.map(m =>
          (m.getName, m.getParameterTypes.map(_.getSimpleName).toList))
      methods shouldBe (
        ("a", Nil) ::
        ("b", Nil) ::
        ("c", Nil) ::
        ("c", List("int")) ::
        ("c", List("int", "int")) ::
        ("c", List("String")) ::
        ("c", List("String", "int")) :: Nil
      )
    }

    "lookup component client instances" in {
      abstract class Foo(val componentClient: ComponentClientImpl)
      class Bar(val anotherComponentClient: ComponentClientImpl, val parentComponentClient: ComponentClientImpl)
          extends Foo(parentComponentClient)

      val c1 = new ComponentClientImpl(null)
      val c2 = new ComponentClientImpl(null)
      val bar = new Bar(c1, c2)

      Reflect.lookupComponentClientField(bar) should have size 2
    }
  }

}