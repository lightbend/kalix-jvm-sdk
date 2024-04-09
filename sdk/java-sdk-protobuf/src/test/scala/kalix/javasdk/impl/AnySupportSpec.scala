/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import kalix.protocol.discovery.{ DiscoveryProto, UserFunctionError }
import kalix.protocol.event_sourced_entity.EventSourcedEntityProto
import com.example.shoppingcart.ShoppingCartApi
import com.google.protobuf.any.{ Any => ScalaPbAny }
import com.google.protobuf.{ Any => JavaPbAny }
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class AnySupportSpec extends AnyWordSpec with Matchers with OptionValues {

  private val anySupport = new AnySupport(
    Array(ShoppingCartApi.getDescriptor, EventSourcedEntityProto.javaDescriptor, DiscoveryProto.javaDescriptor),
    getClass.getClassLoader,
    "com.example")

  private val anySupportScala = new AnySupport(
    Array(ShoppingCartApi.getDescriptor, EventSourcedEntityProto.javaDescriptor, DiscoveryProto.javaDescriptor),
    getClass.getClassLoader,
    "com.example",
    AnySupport.PREFER_SCALA)

  private val addLineItem = ShoppingCartApi.AddLineItem
    .newBuilder()
    .setName("item")
    .setProductId("id")
    .setQuantity(10)
    .build()

  "Any support for Java" should {

    "support se/deserializing java protobufs" in {
      val any = anySupport.encodeScala(addLineItem)
      any.typeUrl should ===("com.example/" + ShoppingCartApi.AddLineItem.getDescriptor.getFullName)
      anySupport.decodePossiblyPrimitive(any) should ===(addLineItem)
    }

    "support se/deserializing scala protobufs" in {
      val error = UserFunctionError("error")
      val any = anySupport.encodeScala(UserFunctionError("error"))
      any.typeUrl should ===("com.example/kalix.protocol.UserFunctionError")

      val decoded = anySupport.decodePossiblyPrimitive(any)
      decoded.getClass should ===(error.getClass)
      decoded should ===(error)
    }

    "support resolving a service descriptor" in {
      val methods =
        anySupport.resolveServiceDescriptor(ShoppingCartApi.getDescriptor.findServiceByName("ShoppingCartService"))
      methods should have size 4
      val method = methods("AddItem")

      // Input type
      val inputAny = anySupport.encodeScala(addLineItem)
      method.inputType.parseFrom(inputAny.value) should ===(addLineItem)

      // Output type - this also checks that when java_multiple_files is true, it works
      val outputAny = anySupport.encodeScala(Empty.getDefaultInstance)
      method.outputType.parseFrom(outputAny.value) should ===(Empty.getDefaultInstance)
    }

    def testPrimitive[T](name: String, value: T, defaultValue: T) = {
      val any = anySupport.encodeScala(value)
      any.typeUrl should ===(AnySupport.KalixPrimitive + name)
      anySupport.decodePossiblyPrimitive(any) should ===(value)

      val defaultAny = anySupport.encodeScala(defaultValue)
      defaultAny.typeUrl should ===(AnySupport.KalixPrimitive + name)
      defaultAny.value.size() shouldBe 0
      anySupport.decodePossiblyPrimitive(defaultAny) should ===(defaultValue)
    }

    "support se/deserializing strings" in testPrimitive("string", "foo", "")
    "support se/deserializing ints" in testPrimitive("int32", 10, 0)
    "support se/deserializing longs" in testPrimitive("int64", 10L, 0L)
    "support se/deserializing floats" in testPrimitive("float", 0.5f, 0f)
    "support se/deserializing doubles" in testPrimitive("double", 0.5d, 0d)
    "support se/deserializing bytes" in testPrimitive("bytes", ByteString.copyFromUtf8("foo"), ByteString.EMPTY)
    "support se/deserializing booleans" in testPrimitive("bool", true, false)

    // note that the StringValue and BytesValue wrapper types are different for Java and Scala and needs to be adapted for Scala
    "deserialize json into StringValue" in {
      val jsonText = """{"such":"json"}"""
      val any =
        ScalaPbAny("json.kalix.io/suffix", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))
      // both as top level message
      val decoded = anySupport.decodeMessage(any)
      decoded shouldBe a[JavaPbAny]
      decoded.asInstanceOf[JavaPbAny].getTypeUrl should ===("json.kalix.io/suffix")
      decoded.asInstanceOf[JavaPbAny].getValue should ===(
        ByteStringEncoding.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))
      val decoded2 = anySupport.decodePossiblyPrimitive(any)
      decoded2.asInstanceOf[JavaPbAny].getTypeUrl should ===("json.kalix.io/suffix")
      decoded2.asInstanceOf[JavaPbAny].getValue should ===(
        ByteStringEncoding.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))
    }

    "deserialize text into StringValue" in {
      val plainText = "some text"
      val any =
        ScalaPbAny("type.kalix.io/string", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(plainText)))
      // both as top level message
      val decoded = anySupport.decodeMessage(any)
      decoded shouldBe a[com.google.protobuf.StringValue]
      decoded.asInstanceOf[com.google.protobuf.StringValue].getValue should ===(plainText)
      val decoded2 = anySupport.decodePossiblyPrimitive(any)
      decoded2 shouldBe a[String]
    }

    "deserialize bytes into BytesValue" in {
      val bytes = "some texty bytes"
      val any =
        ScalaPbAny("type.kalix.io/bytes", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(bytes)))
      // both as top level message
      val decoded = anySupport.decodeMessage(any)
      decoded shouldBe a[com.google.protobuf.BytesValue]
      decoded.asInstanceOf[com.google.protobuf.BytesValue].getValue.toStringUtf8 should ===(bytes)
      val decoded2 = anySupport.decodePossiblyPrimitive(any)
      decoded2 shouldBe a[ByteString]
    }

    "serialize BytesValue like a regular message" in {
      val bytes = ByteString.copyFromUtf8("woho!")
      val encoded = anySupport.encodeScala(com.google.protobuf.BytesValue.newBuilder().setValue(bytes).build())
      encoded.typeUrl should ===("type.googleapis.com/google.protobuf.BytesValue")
      com.google.protobuf.BytesValue.parseFrom(encoded.value).getValue should ===(bytes)
    }

    "serialize StringValue like a regular message" in {
      val text = "waha!"
      val encoded = anySupport.encodeScala(com.google.protobuf.StringValue.newBuilder().setValue(text).build())
      encoded.typeUrl should ===("type.googleapis.com/google.protobuf.StringValue")
      com.google.protobuf.StringValue.parseFrom(encoded.value).getValue should ===(text)
    }
  }

  "Any support for Scala" should {

    // note that the StringValue and BytesValue wrapper types are different for Java and Scala and needs to be adapted for Scala
    "pass on json as is" in {
      val jsonText = """{"such":"json"}"""
      val any =
        ScalaPbAny("json.kalix.io/suffix", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))
      // both as top level message
      val decoded = anySupportScala.decodeMessage(any)
      decoded shouldBe a[ScalaPbAny]
      // kept to allow user to distinguish different messages based on suffix
      decoded.asInstanceOf[ScalaPbAny].typeUrl should ===("json.kalix.io/suffix")
      decoded.asInstanceOf[ScalaPbAny].value should ===(
        ByteStringEncoding.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))

      val decoded2 = anySupportScala.decodePossiblyPrimitive(any)
      decoded2.asInstanceOf[ScalaPbAny].typeUrl should ===("json.kalix.io/suffix")
      decoded2.asInstanceOf[ScalaPbAny].value should ===(
        ByteStringEncoding.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))

    }

    "deserialize text into StringValue" in {
      val plainText = "some text"
      val any =
        ScalaPbAny("type.kalix.io/string", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(plainText)))
      // both as top level message
      val decoded = anySupportScala.decodeMessage(any)
      decoded shouldBe a[com.google.protobuf.wrappers.StringValue]
      decoded.asInstanceOf[com.google.protobuf.wrappers.StringValue].value should ===(plainText)
      val decoded2 = anySupportScala.decodePossiblyPrimitive(any)
      decoded2 shouldBe a[String]
    }

    "deserialize bytes into BytesValue" in {
      val bytes = "some texty bytes"
      val any =
        ScalaPbAny("type.kalix.io/bytes", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(bytes)))
      // both as top level message
      val decoded = anySupportScala.decodeMessage(any)
      decoded shouldBe a[com.google.protobuf.wrappers.BytesValue]
      decoded.asInstanceOf[com.google.protobuf.wrappers.BytesValue].value.toStringUtf8 should ===(bytes)
      val decoded2 = anySupportScala.decodePossiblyPrimitive(any)
      decoded2 shouldBe a[ByteString]
    }

    "serialize BytesValue like a regular message" in {
      val bytes = ByteString.copyFromUtf8("woho!")
      val encoded = anySupport.encodeScala(com.google.protobuf.wrappers.BytesValue(bytes))
      encoded.typeUrl should ===("type.googleapis.com/google.protobuf.BytesValue")
      com.google.protobuf.wrappers.BytesValue.parseFrom(encoded.value.newCodedInput()).value should ===(bytes)
    }

    "serialize StringValue like a regular message" in {
      val text = "waha!"
      val encoded = anySupport.encodeScala(com.google.protobuf.wrappers.StringValue(text))
      encoded.typeUrl should ===("type.googleapis.com/google.protobuf.StringValue")
      com.google.protobuf.wrappers.StringValue.parseFrom(encoded.value.newCodedInput()).value should ===(text)
    }
  }

}
