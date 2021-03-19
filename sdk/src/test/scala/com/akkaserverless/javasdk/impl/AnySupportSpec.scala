/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl

import com.akkaserverless.javasdk.Jsonable
import com.akkaserverless.protocol.discovery.{DiscoveryProto, UserFunctionError}
import com.akkaserverless.protocol.event_sourced_entity.EventSourcedEntityProto
import com.example.shoppingcart.ShoppingCart
import com.google.protobuf.{ByteString, Empty}
import org.scalatest.OptionValues
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import scala.beans.BeanProperty

class AnySupportSpec extends AnyWordSpec with Matchers with OptionValues {

  private val anySupport = new AnySupport(
    Array(ShoppingCart.getDescriptor, EventSourcedEntityProto.javaDescriptor, DiscoveryProto.javaDescriptor),
    getClass.getClassLoader,
    "com.example"
  )
  private val addLineItem = ShoppingCart.AddLineItem
    .newBuilder()
    .setName("item")
    .setProductId("id")
    .setQuantity(10)
    .build()

  "Any support" should {

    "support se/deserializing java protobufs" in {
      val any = anySupport.encodeScala(addLineItem)
      any.typeUrl should ===("com.example/" + ShoppingCart.AddLineItem.getDescriptor.getFullName)
      anySupport.decode(any) should ===(addLineItem)
    }

    "support se/deserializing scala protobufs" in {
      val error = UserFunctionError("error")
      val any = anySupport.encodeScala(UserFunctionError("error"))
      any.typeUrl should ===("com.example/akkaserverless.UserFunctionError")

      val decoded = anySupport.decode(any)
      decoded.getClass should ===(error.getClass)
      decoded should ===(error)
    }

    "support resolving a service descriptor" in {
      val methods =
        anySupport.resolveServiceDescriptor(ShoppingCart.getDescriptor.findServiceByName("ShoppingCartService"))
      methods should have size 3
      val method = methods("AddItem")

      // Input type
      method.inputType.typeUrl should ===("com.example/" + ShoppingCart.AddLineItem.getDescriptor.getFullName)
      method.inputType.typeClass should ===(classOf[ShoppingCart.AddLineItem])
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
      anySupport.decode(any) should ===(value)

      val defaultAny = anySupport.encodeScala(defaultValue)
      defaultAny.typeUrl should ===(AnySupport.AkkaServerlessPrimitive + name)
      defaultAny.value.size() shouldBe 0
      anySupport.decode(defaultAny) should ===(defaultValue)
    }

    "support se/deserializing strings" in testPrimitive("string", "foo", "")
    "support se/deserializing ints" in testPrimitive("int32", 10, 0)
    "support se/deserializing longs" in testPrimitive("int64", 10L, 0L)
    "support se/deserializing floats" in testPrimitive("float", 0.5f, 0f)
    "support se/deserializing doubles" in testPrimitive("double", 0.5d, 0d)
    "support se/deserializing bytes" in testPrimitive("bytes", ByteString.copyFromUtf8("foo"), ByteString.EMPTY)
    "support se/deserializing booleans" in testPrimitive("bool", true, false)

    "support se/deserializing json" in {
      val myJsonable = new MyJsonable
      myJsonable.field = "foo"
      val any = anySupport.encodeScala(myJsonable)
      any.typeUrl should ===(AnySupport.AkkaServerlessJson + classOf[MyJsonable].getName)
      anySupport.decode(any).asInstanceOf[MyJsonable].field should ===("foo")
    }

  }

}

@Jsonable
class MyJsonable {
  @BeanProperty var field: String = _
}
