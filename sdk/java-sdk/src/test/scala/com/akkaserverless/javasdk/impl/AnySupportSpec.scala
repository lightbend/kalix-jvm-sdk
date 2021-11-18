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

package com.akkaserverless.javasdk.impl

import com.akkaserverless.javasdk.JsonSupport
import com.akkaserverless.protocol.discovery.{ DiscoveryProto, UserFunctionError }
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedEntityProto
import com.example.shoppingcart.ShoppingCartApi
import com.google.protobuf.{ ByteString, Empty }
import com.google.protobuf.{ Any => JavaPbAny }
import com.google.protobuf.any.{ Any => ScalaPbAny }
import org.scalatest.OptionValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

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
      any.typeUrl should ===("com.example/akkaserverless.protocol.UserFunctionError")

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
      method.inputType.typeUrl should ===("com.example/" + ShoppingCartApi.AddLineItem.getDescriptor.getFullName)
      method.inputType.typeClass should ===(classOf[ShoppingCartApi.AddLineItem])
      val iBytes = method.inputType.asInstanceOf[ResolvedType[Any]].toByteString(addLineItem)
      method.inputType.parseFrom(iBytes) should ===(addLineItem)

      // Output type - this also checks that when java_multiple_files is true, it works
      method.outputType.typeUrl should ===("com.example/" + Empty.getDescriptor.getFullName)
      method.outputType.typeClass should ===(classOf[Empty])
      val oBytes = method.outputType.asInstanceOf[ResolvedType[Any]].toByteString(Empty.getDefaultInstance)
      method.outputType.parseFrom(oBytes) should ===(Empty.getDefaultInstance)
    }

    def testPrimitive[T](name: String, value: T, defaultValue: T) = {
      val any = anySupport.encodeScala(value)
      any.typeUrl should ===(AnySupport.AkkaServerlessPrimitive + name)
      anySupport.decodePossiblyPrimitive(any) should ===(value)

      val defaultAny = anySupport.encodeScala(defaultValue)
      defaultAny.typeUrl should ===(AnySupport.AkkaServerlessPrimitive + name)
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
        ScalaPbAny("json.akkaserverless.com/suffix", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))
      // both as top level message
      val decoded = anySupport.decodeMessage(any)
      decoded shouldBe a[JavaPbAny]
      decoded.asInstanceOf[JavaPbAny].getTypeUrl should ===("json.akkaserverless.com/suffix")
      decoded.asInstanceOf[JavaPbAny].getValue should ===(
        ByteStringEncoding.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))
      val decoded2 = anySupport.decodePossiblyPrimitive(any)
      decoded2.asInstanceOf[JavaPbAny].getTypeUrl should ===("json.akkaserverless.com/suffix")
      decoded2.asInstanceOf[JavaPbAny].getValue should ===(
        ByteStringEncoding.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))
    }

    "deserialize text into StringValue" in {
      val plainText = "some text"
      val any =
        ScalaPbAny("p.akkaserverless.com/string", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(plainText)))
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
        ScalaPbAny("p.akkaserverless.com/bytes", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(bytes)))
      // both as top level message
      val decoded = anySupport.decodeMessage(any)
      decoded shouldBe a[com.google.protobuf.BytesValue]
      decoded.asInstanceOf[com.google.protobuf.BytesValue].getValue.toStringUtf8 should ===(bytes)
      val decoded2 = anySupport.decodePossiblyPrimitive(any)
      decoded2 shouldBe a[ByteString]
    }
  }

  "Any support for Scala" should {

    // note that the StringValue and BytesValue wrapper types are different for Java and Scala and needs to be adapted for Scala
    "pass on json as is" in {
      val jsonText = """{"such":"json"}"""
      val any =
        ScalaPbAny("json.akkaserverless.com/suffix", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))
      // both as top level message
      val decoded = anySupportScala.decodeMessage(any)
      decoded shouldBe a[ScalaPbAny]
      // kept to allow user to distinguish different messages based on suffix
      decoded.asInstanceOf[ScalaPbAny].typeUrl should ===("json.akkaserverless.com/suffix")
      decoded.asInstanceOf[ScalaPbAny].value should ===(
        ByteStringEncoding.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))

      val decoded2 = anySupportScala.decodePossiblyPrimitive(any)
      decoded2.asInstanceOf[ScalaPbAny].typeUrl should ===("json.akkaserverless.com/suffix")
      decoded2.asInstanceOf[ScalaPbAny].value should ===(
        ByteStringEncoding.encodePrimitiveBytes(ByteString.copyFromUtf8(jsonText)))

    }

    "deserialize text into StringValue" in {
      val plainText = "some text"
      val any =
        ScalaPbAny("p.akkaserverless.com/string", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(plainText)))
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
        ScalaPbAny("p.akkaserverless.com/bytes", AnySupport.encodePrimitiveBytes(ByteString.copyFromUtf8(bytes)))
      // both as top level message
      val decoded = anySupportScala.decodeMessage(any)
      decoded shouldBe a[com.google.protobuf.wrappers.BytesValue]
      decoded.asInstanceOf[com.google.protobuf.wrappers.BytesValue].value.toStringUtf8 should ===(bytes)
      val decoded2 = anySupportScala.decodePossiblyPrimitive(any)
      decoded2 shouldBe a[ByteString]
    }

  }

}
