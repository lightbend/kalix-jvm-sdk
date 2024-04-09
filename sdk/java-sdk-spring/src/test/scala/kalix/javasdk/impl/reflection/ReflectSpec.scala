/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.reflection

import kalix.javasdk.client.ComponentClient
import kalix.javasdk.impl.client.ComponentClientImpl
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
      abstract class Foo(val componentClient: ComponentClient)
      class Bar(val anotherComponentClient: ComponentClient, val parentComponentClient: ComponentClient)
          extends Foo(parentComponentClient)

      val c1 = new ComponentClientImpl(null)
      val c2 = new ComponentClientImpl(null)
      val bar = new Bar(c1, c2)

      Reflect.lookupComponentClientFields(bar) should have size 2
    }
  }
}
