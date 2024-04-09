/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import java.lang
import java.util

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import kalix.javasdk.JsonMigration
import kalix.javasdk.JsonSupport
import kalix.javasdk.annotations.Migration
import kalix.javasdk.annotations.TypeName
import kalix.javasdk.impl.JsonMessageCodecSpec.Cat
import kalix.javasdk.impl.JsonMessageCodecSpec.Dog
import kalix.javasdk.impl.JsonMessageCodecSpec.SimpleClass
import kalix.javasdk.impl.JsonMessageCodecSpec.SimpleClassUpdated
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

object JsonMessageCodecSpec {

  @JsonCreator
  @TypeName("animal")
  case class Dog(str: String)

  @JsonCreator
  @TypeName("animal")
  case class Cat(str: String)

  @JsonCreator
  case class SimpleClass(str: String, in: Int)

  class SimpleClassUpdatedMigration extends JsonMigration {
    override def currentVersion(): Int = 1
    override def transform(fromVersion: Int, jsonNode: JsonNode): JsonNode = {
      if (fromVersion == 0) {
        jsonNode.asInstanceOf[ObjectNode].set("newField", IntNode.valueOf(1))
      } else {
        jsonNode
      }
    }

    override def supportedClassNames(): util.List[String] = {
      util.List.of(classOf[SimpleClass].getName)
    }
  }

  @JsonCreator
  @Migration(classOf[SimpleClassUpdatedMigration])
  case class SimpleClassUpdated(str: String, in: Int, newField: Int)

  object AnnotatedWithTypeName {

    sealed trait Animal

    @TypeName("lion")
    final case class Lion(name: String) extends Animal

    @TypeName("elephant")
    final case class Elephant(name: String, age: Int) extends Animal

    @TypeName("elephant")
    final case class IndianElephant(name: String, age: Int) extends Animal
  }

  object AnnotatedWithEmptyTypeName {

    sealed trait Animal

    @TypeName("")
    final case class Lion(name: String) extends Animal

    @TypeName(" ")
    final case class Elephant(name: String, age: Int) extends Animal
  }

}
class JsonMessageCodecSpec extends AnyWordSpec with Matchers {

  def jsonTypeUrlWith(typ: String) = JsonSupport.KALIX_JSON + typ

  val messageCodec = new JsonMessageCodec

  "The JsonMessageCodec" should {

    "check java primitives backward compatibility" in {
      val integer = messageCodec.encodeScala(123)
      integer.typeUrl shouldBe jsonTypeUrlWith("int")
      new StrictJsonMessageCodec(messageCodec).decodeMessage(
        integer.copy(typeUrl = jsonTypeUrlWith("java.lang.Integer"))) shouldBe 123

      val long = messageCodec.encodeScala(123L)
      long.typeUrl shouldBe jsonTypeUrlWith("long")
      new StrictJsonMessageCodec(messageCodec).decodeMessage(
        long.copy(typeUrl = jsonTypeUrlWith("java.lang.Long"))) shouldBe 123

      val string = messageCodec.encodeScala("123")
      string.typeUrl shouldBe jsonTypeUrlWith("string")
      new StrictJsonMessageCodec(messageCodec).decodeMessage(
        string.copy(typeUrl = jsonTypeUrlWith("java.lang.String"))) shouldBe "123"

      val boolean = messageCodec.encodeScala(true)
      boolean.typeUrl shouldBe jsonTypeUrlWith("boolean")
      new StrictJsonMessageCodec(messageCodec).decodeMessage(
        boolean.copy(typeUrl = jsonTypeUrlWith("java.lang.Boolean"))) shouldBe true

      val double = messageCodec.encodeScala(123.321d)
      double.typeUrl shouldBe jsonTypeUrlWith("double")
      new StrictJsonMessageCodec(messageCodec).decodeMessage(
        double.copy(typeUrl = jsonTypeUrlWith("java.lang.Double"))) shouldBe 123.321d

      val float = messageCodec.encodeScala(123.321f)
      float.typeUrl shouldBe jsonTypeUrlWith("float")
      new StrictJsonMessageCodec(messageCodec).decodeMessage(
        float.copy(typeUrl = jsonTypeUrlWith("java.lang.Float"))) shouldBe 123.321f

      val short = messageCodec.encodeScala(lang.Short.valueOf("1"))
      short.typeUrl shouldBe jsonTypeUrlWith("short")
      new StrictJsonMessageCodec(messageCodec).decodeMessage(
        short.copy(typeUrl = jsonTypeUrlWith("java.lang.Short"))) shouldBe lang.Short.valueOf("1")

      val char = messageCodec.encodeScala('a')
      char.typeUrl shouldBe jsonTypeUrlWith("char")
      new StrictJsonMessageCodec(messageCodec).decodeMessage(
        char.copy(typeUrl = jsonTypeUrlWith("java.lang.Character"))) shouldBe 'a'

      val byte = messageCodec.encodeScala(1.toByte)
      byte.typeUrl shouldBe jsonTypeUrlWith("byte")
      new StrictJsonMessageCodec(messageCodec).decodeMessage(
        byte.copy(typeUrl = jsonTypeUrlWith("java.lang.Byte"))) shouldBe 1.toByte
    }

    "default to FQCN for typeUrl (java)" in {
      val encoded = messageCodec.encodeJava(SimpleClass("abc", 10))
      encoded.getTypeUrl shouldBe jsonTypeUrlWith("kalix.javasdk.impl.JsonMessageCodecSpec$SimpleClass")
    }

    "add version number to typeUrl" in {
      //new codec to avoid collision with SimpleClass
      val encoded = new JsonMessageCodec().encodeJava(SimpleClassUpdated("abc", 10, 123))
      encoded.getTypeUrl shouldBe jsonTypeUrlWith("kalix.javasdk.impl.JsonMessageCodecSpec$SimpleClassUpdated#1")
    }

    "decode with new schema version" in {
      val encoded = messageCodec.encodeJava(SimpleClass("abc", 10))
      val decoded =
        JsonSupport.decodeJson(classOf[SimpleClassUpdated], encoded)
      decoded shouldBe SimpleClassUpdated("abc", 10, 1)
    }

    "not re-encode (wrap) to JavaPbAny" in {
      val encoded: JavaPbAny = messageCodec.encodeJava(SimpleClass("abc", 10))
      val reEncoded = messageCodec.encodeJava(encoded)
      reEncoded shouldBe encoded
    }

    "not re-encode (wrap) from ScalaPbAny to JavaPbAny" in {
      val encoded: ScalaPbAny = messageCodec.encodeScala(SimpleClass("abc", 10))
      val reEncoded = messageCodec.encodeJava(encoded)
      reEncoded shouldBe an[JavaPbAny]
      reEncoded.getTypeUrl shouldBe encoded.typeUrl
      reEncoded.getValue shouldBe encoded.value
    }

    "default to FQCN for typeUrl (scala)" in {
      val encoded = messageCodec.encodeScala(SimpleClass("abc", 10))
      encoded.typeUrl shouldBe jsonTypeUrlWith("kalix.javasdk.impl.JsonMessageCodecSpec$SimpleClass")
    }

    "not re-encode (wrap) to ScalaPbAny" in {
      val encoded: ScalaPbAny = messageCodec.encodeScala(SimpleClass("abc", 10))
      val reEncoded = messageCodec.encodeScala(encoded)
      reEncoded shouldBe encoded
    }

    "not re-encode (wrap) from JavaPbAny to ScalaPbAny" in {
      val encoded: JavaPbAny = messageCodec.encodeJava(SimpleClass("abc", 10))
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

      val decoded = new StrictJsonMessageCodec(messageCodec).decodeMessage(encoded)

      decoded shouldBe value
    }

    "decode message with new version" in {
      //old schema
      val value = SimpleClass("abc", 10)
      val encoded = new JsonMessageCodec().encodeScala(value)

      //new schema, simulating restart
      val messageCodecAfterRestart = new JsonMessageCodec()
      messageCodecAfterRestart.typeUrlFor(classOf[SimpleClassUpdated])
      val decoded = new StrictJsonMessageCodec(messageCodecAfterRestart).decodeMessage(encoded)

      decoded shouldBe SimpleClassUpdated(value.str, value.in, 1)
    }

    {
      import JsonMessageCodecSpec.AnnotatedWithTypeName.Elephant
      import JsonMessageCodecSpec.AnnotatedWithTypeName.IndianElephant
      import JsonMessageCodecSpec.AnnotatedWithTypeName.Lion

      "fail when using the same TypeName" in {
        val encodedElephant = messageCodec.encodeJava(Elephant("Dumbo", 1))
        encodedElephant.getTypeUrl shouldBe jsonTypeUrlWith("elephant")

        val exception = intercept[IllegalStateException] {
          messageCodec.encodeJava(IndianElephant("Dumbo", 1))
        }

        exception.getMessage shouldBe "Collision with existing existing mapping class kalix.javasdk.impl.JsonMessageCodecSpec$AnnotatedWithTypeName$Elephant -> elephant. The same type name can't be used for other class class kalix.javasdk.impl.JsonMessageCodecSpec$AnnotatedWithTypeName$IndianElephant"
      }

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
      import JsonMessageCodecSpec.AnnotatedWithEmptyTypeName.Elephant
      import JsonMessageCodecSpec.AnnotatedWithEmptyTypeName.Lion

      "default to FQCN  if TypeName has empty string (java)" in {

        val encodedLion = messageCodec.encodeJava(Lion("Simba"))
        encodedLion.getTypeUrl shouldBe jsonTypeUrlWith(
          "kalix.javasdk.impl.JsonMessageCodecSpec$AnnotatedWithEmptyTypeName$Lion")

        val encodedElephant = messageCodec.encodeJava(Elephant("Dumbo", 1))
        encodedElephant.getTypeUrl shouldBe jsonTypeUrlWith(
          "kalix.javasdk.impl.JsonMessageCodecSpec$AnnotatedWithEmptyTypeName$Elephant")
      }

      "default to FQCN  if TypeName has empty string" in {

        val encodedLion = messageCodec.encodeScala(Lion("Simba"))
        encodedLion.typeUrl shouldBe jsonTypeUrlWith(
          "kalix.javasdk.impl.JsonMessageCodecSpec$AnnotatedWithEmptyTypeName$Lion")

        val encodedElephant = messageCodec.encodeScala(Elephant("Dumbo", 1))
        encodedElephant.typeUrl shouldBe jsonTypeUrlWith(
          "kalix.javasdk.impl.JsonMessageCodecSpec$AnnotatedWithEmptyTypeName$Elephant")
      }
    }

    "throw if receiving null (scala)" in {
      val failed = intercept[RuntimeException] {
        messageCodec.encodeScala(null)
      }
      failed.getMessage shouldBe "Don't know how to serialize object of type null."
    }

    "throw if receiving null (java)" in {
      val failed = intercept[RuntimeException] {
        messageCodec.encodeJava(null)
      }
      failed.getMessage shouldBe "Don't know how to serialize object of type null."
    }
  }
}
