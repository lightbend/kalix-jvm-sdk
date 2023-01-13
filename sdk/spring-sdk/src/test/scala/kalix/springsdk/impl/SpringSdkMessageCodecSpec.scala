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
import kalix.springsdk.annotations.TypeName
import kalix.springsdk.impl.SpringSdkMessageCodecSpec.SimpleClass
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.springsdk.impl.SpringSdkMessageCodecSpec.Cat
import kalix.springsdk.impl.SpringSdkMessageCodecSpec.Dog

object SpringSdkMessageCodecSpec {

  @JsonCreator
  @TypeName("animal")
  case class Dog(str: String)

  @JsonCreator
  @TypeName("animal")
  case class Cat(str: String)

  @JsonCreator
  case class SimpleClass(str: String, in: Int)

  object AnnotatedWithTypeName {

    sealed trait Animal

    @TypeName("lion")
    final case class Lion(name: String) extends Animal

    @TypeName("elephant")
    final case class Elephant(name: String, age: Int) extends Animal
  }

  object AnnotatedWithEmptyTypeName {

    sealed trait Animal

    @TypeName("")
    final case class Lion(name: String) extends Animal

    @TypeName(" ")
    final case class Elephant(name: String, age: Int) extends Animal
  }

}
class SpringSdkMessageCodecSpec extends AnyWordSpec with Matchers {

  def jsonTypeUrlWith(typ: String) = JsonSupport.KALIX_JSON + typ

  val messageCodec = new SpringSdkMessageCodec

  "The SpringSdkMessageCodec" should {

    "default to FQCN for typeUrl (java)" in {
      val encoded = messageCodec.encodeJava(SimpleClass("abc", 10))
      encoded.getTypeUrl shouldBe jsonTypeUrlWith("SimpleClass")
    }

    "not encode java twice" in {
      val encoded = messageCodec.encodeJava(SimpleClass("abc", 10))
      val reEncoded = messageCodec.encodeJava(encoded)
      reEncoded shouldBe encoded
    }

    "not encode scala to java" in {
      val encoded = messageCodec.encodeScala(SimpleClass("abc", 10))
      val reEncoded = messageCodec.encodeJava(encoded)
      reEncoded shouldBe an[JavaPbAny]
      reEncoded.getTypeUrl shouldBe encoded.typeUrl
      reEncoded.getValue shouldBe encoded.value
    }

    "default to FQCN for typeUrl (scala)" in {
      val encoded = messageCodec.encodeScala(SimpleClass("abc", 10))
      encoded.typeUrl shouldBe jsonTypeUrlWith("SimpleClass")
    }

    "not encode scala twice" in {
      val encoded = messageCodec.encodeScala(SimpleClass("abc", 10))
      val reEncoded = messageCodec.encodeScala(encoded)
      reEncoded shouldBe encoded
    }

    "not encode java to scala" in {
      val encoded = messageCodec.encodeJava(SimpleClass("abc", 10))
      val reEncoded = messageCodec.encodeScala(encoded)
      reEncoded shouldBe an[ScalaPbAny]
      reEncoded.typeUrl shouldBe encoded.getTypeUrl
      reEncoded.value shouldBe encoded.getValue
    }

    "fail with the same" in {
      //fill the cache
      messageCodec.encodeJava(Dog("abc"))
      assertThrows[IllegalStateException] {
        messageCodec.encodeJava(Cat("abc"))
      }
    }

    "decode message" in {
      val value = SimpleClass("abc", 10)
      val encoded = messageCodec.encodeScala(value)

      val decoded = messageCodec.decodeToJson(encoded)

      decoded shouldBe value
    }

    {
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWithTypeName.Elephant
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWithTypeName.Lion

      "use TypeName if available (java)" in {

        val encodedLion = messageCodec.encodeJava(Lion("Simba"))
        encodedLion.getTypeUrl shouldBe jsonTypeUrlWith("lion")

        val encodedElephant = messageCodec.encodeJava(Elephant("Dumbo", 1))
        encodedElephant.getTypeUrl shouldBe jsonTypeUrlWith("elephant")
      }

      "use TypeName if available  (scala)" in {

        val encodedLion = messageCodec.encodeScala(Lion("Simba"))
        encodedLion.typeUrl shouldBe jsonTypeUrlWith("lion")

        val encodedElephant = messageCodec.encodeScala(Elephant("Dumbo", 1))
        encodedElephant.typeUrl shouldBe jsonTypeUrlWith("elephant")
      }
    }

    {
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWithEmptyTypeName.Elephant
      import kalix.springsdk.impl.SpringSdkMessageCodecSpec.AnnotatedWithEmptyTypeName.Lion

      "default to FQCN  if TypeName is has empty string (java)" in {

        val encodedLion = messageCodec.encodeJava(Lion("Simba"))
        encodedLion.getTypeUrl shouldBe jsonTypeUrlWith("Lion")

        val encodedElephant = messageCodec.encodeJava(Elephant("Dumbo", 1))
        encodedElephant.getTypeUrl shouldBe jsonTypeUrlWith("Elephant")
      }

      "default to FQCN  if TypeName is has empty string" in {

        val encodedLion = messageCodec.encodeScala(Lion("Simba"))
        encodedLion.typeUrl shouldBe jsonTypeUrlWith("Lion")

        val encodedElephant = messageCodec.encodeScala(Elephant("Dumbo", 1))
        encodedElephant.typeUrl shouldBe jsonTypeUrlWith("Elephant")
      }
    }
  }
}
