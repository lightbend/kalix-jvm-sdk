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

package kalix.springsdk.impl

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import kalix.javasdk.JsonSupport
import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWithSingleName.Elephant
import kalix.springsdk.impl.SpringSdkMessageCodecSpec.SimpleClass
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

object SpringSdkMessageCodecSpec {

  @JsonCreator
  case class SimpleClass(str: String, in: Int)

  object AnnotatedWithSingleName {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
      Array(
        new JsonSubTypes.Type(value = classOf[Lion], name = "lion"),
        new JsonSubTypes.Type(value = classOf[Elephant], name = "elephant")))
    sealed trait Animal

    final case class Lion(name: String) extends Animal

    final case class Elephant(name: String, age: Int) extends Animal

  }

  object AnnotatedWithManyNames {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
      Array(
        new JsonSubTypes.Type(value = classOf[Lion], names = Array("lion", "noil")),
        new JsonSubTypes.Type(value = classOf[Elephant], names = Array("elephant", "tnahpele"))))
    sealed trait Animal

    final case class Lion(name: String) extends Animal

    final case class Elephant(name: String, age: Int) extends Animal

  }
  object AnnotatedWIthJsonTypeName {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(Array(new JsonSubTypes.Type(value = classOf[Lion]), new JsonSubTypes.Type(value = classOf[Elephant])))
    sealed trait Animal

    @JsonTypeName("lion")
    final case class Lion(name: String) extends Animal
    @JsonTypeName("elephant")
    final case class Elephant(name: String, age: Int) extends Animal
  }

  object HierarchyWithAbstractClass {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    @JsonSubTypes(
      Array(
        new JsonSubTypes.Type(value = classOf[Lion], name = "lion"),
        new JsonSubTypes.Type(value = classOf[Elephant], name = "elephant")))
    abstract class Animal

    final case class Lion(name: String) extends Animal

    final case class Elephant(name: String, age: Int) extends Animal

  }
}
class SpringSdkMessageCodecSpec extends AnyWordSpec with Matchers {

  def jsonTypeUrlWith(typ: String) = JsonSupport.KALIX_JSON + typ

  "The SpringSdkMessageCodec" should {

    "by default FQCN for typeUrl (java)" in {
      val encoded = SpringSdkMessageCodec.encodeJava(SimpleClass("abc", 10))
      encoded.getTypeUrl shouldBe jsonTypeUrlWith("kalix.springsdk.impl.SpringSdkMessageCodecSpec$SimpleClass")
    }

    "by default FQCN for typeUrl (scala)" in {
      val encoded = SpringSdkMessageCodec.encodeScala(SimpleClass("abc", 10))
      encoded.typeUrl shouldBe jsonTypeUrlWith("kalix.springsdk.impl.SpringSdkMessageCodecSpec$SimpleClass")
    }

    {
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWIthJsonTypeName.Lion
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWIthJsonTypeName.Elephant

      "use JsonTypeName if available (java)" in {

        val encodedLion = SpringSdkMessageCodec.encodeJava(Lion("Simba"))
        encodedLion.getTypeUrl shouldBe jsonTypeUrlWith("lion")

        val encodedElephant = SpringSdkMessageCodec.encodeJava(Elephant("Dumbo", 1))
        encodedElephant.getTypeUrl shouldBe jsonTypeUrlWith("elephant")
      }

      "use JsonTypeName if available  (scala)" in {

        val encodedLion = SpringSdkMessageCodec.encodeScala(Lion("Simba"))
        encodedLion.typeUrl shouldBe jsonTypeUrlWith("lion")

        val encodedElephant = SpringSdkMessageCodec.encodeScala(Elephant("Dumbo", 1))
        encodedElephant.typeUrl shouldBe jsonTypeUrlWith("elephant")
      }
    }

    {
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWithSingleName.Elephant
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWithSingleName.Lion

      "use JsonTypeInfo if available (java)" in {

        val encodedLion = SpringSdkMessageCodec.encodeJava(Lion("Simba"))
        encodedLion.getTypeUrl shouldBe jsonTypeUrlWith("lion")

        val encodedElephant = SpringSdkMessageCodec.encodeJava(Elephant("Dumbo", 1))
        encodedElephant.getTypeUrl shouldBe jsonTypeUrlWith("elephant")
      }

      "use JsonTypeInfo if available (scala)" in {

        val encodedLion = SpringSdkMessageCodec.encodeScala(Lion("Simba"))
        encodedLion.typeUrl shouldBe jsonTypeUrlWith("lion")

        val encodedElephant = SpringSdkMessageCodec.encodeScala(Elephant("Dumbo", 1))
        encodedElephant.typeUrl shouldBe jsonTypeUrlWith("elephant")
      }
    }

    {
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWithManyNames.Lion
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWithManyNames.Elephant

      "use JsonTypeInfo first name if multiple names are available (java)" in {

        val encodedLion = SpringSdkMessageCodec.encodeJava(Lion("Simba"))
        encodedLion.getTypeUrl shouldBe jsonTypeUrlWith("lion")

        val encodedElephant = SpringSdkMessageCodec.encodeJava(Elephant("Dumbo", 1))
        encodedElephant.getTypeUrl shouldBe jsonTypeUrlWith("elephant")
      }

      "use JsonTypeInfo first name if multiple names are available (scala)" in {

        val encodedLion = SpringSdkMessageCodec.encodeScala(Lion("Simba"))
        encodedLion.typeUrl shouldBe jsonTypeUrlWith("lion")

        val encodedElephant = SpringSdkMessageCodec.encodeScala(Elephant("Dumbo", 1))
        encodedElephant.typeUrl shouldBe jsonTypeUrlWith("elephant")
      }
    }

    {
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.HierarchyWithAbstractClass.Elephant
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.HierarchyWithAbstractClass.Lion

      "use JsonTypeInfo if available in abstract class parent (java)" in {

        val encodedLion = SpringSdkMessageCodec.encodeJava(Lion("Simba"))
        encodedLion.getTypeUrl shouldBe jsonTypeUrlWith("lion")

        val encodedElephant = SpringSdkMessageCodec.encodeJava(Elephant("Dumbo", 1))
        encodedElephant.getTypeUrl shouldBe jsonTypeUrlWith("elephant")
      }

      "use JsonTypeInfo if available in abstract class parent (scala)" in {

        val encodedLion = SpringSdkMessageCodec.encodeScala(Lion("Simba"))
        encodedLion.typeUrl shouldBe jsonTypeUrlWith("lion")

        val encodedElephant = SpringSdkMessageCodec.encodeScala(Elephant("Dumbo", 1))
        encodedElephant.typeUrl shouldBe jsonTypeUrlWith("elephant")
      }
    }

  }
}
