/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk

import java.util
import java.util.Optional

import scala.beans.BeanProperty

import akka.Done
import com.google.protobuf.Any
import com.google.protobuf.UnsafeByteOperations
import kalix.javasdk.impl.ByteStringEncoding
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class MyJsonable {
  @BeanProperty var field: String = _
}

class JsonSupportSpec extends AnyWordSpec with Matchers {

  val myJsonable = new MyJsonable
  myJsonable.field = "foo"

  "JsonSupport" must {

    "serialize and deserialize JSON" in {
      val any = JsonSupport.encodeJson(myJsonable)
      any.getTypeUrl should ===(JsonSupport.KALIX_JSON + classOf[MyJsonable].getName)
      JsonSupport.decodeJson(classOf[MyJsonable], any).field should ===("foo")
    }

    "serialize and deserialize DummyClass" in {
      val dummyClass = new DummyClass("123", 321, Optional.of("test"))
      val any = JsonSupport.encodeJson(dummyClass)
      any.getTypeUrl should ===(JsonSupport.KALIX_JSON + classOf[DummyClass].getName)
      val decoded = JsonSupport.decodeJson(classOf[DummyClass], any)
      decoded shouldBe dummyClass
    }

    "deserialize missing field as optional none" in {
      val bytes = UnsafeByteOperations.unsafeWrap("""{"stringValue":"123","intValue":321}""".getBytes)
      val encodedBytes = ByteStringEncoding.encodePrimitiveBytes(bytes)
      val any =
        Any.newBuilder.setTypeUrl(JsonSupport.KALIX_JSON + classOf[DummyClass].getName).setValue(encodedBytes).build

      val decoded = JsonSupport.decodeJson(classOf[DummyClass], any)
      decoded shouldBe new DummyClass("123", 321, Optional.empty())
    }

    "deserialize null field as optional none" in {
      val bytes =
        UnsafeByteOperations.unsafeWrap("""{"stringValue":"123","intValue":321,"optionalStringValue":null}""".getBytes)
      val encodedBytes = ByteStringEncoding.encodePrimitiveBytes(bytes)
      val any =
        Any.newBuilder.setTypeUrl(JsonSupport.KALIX_JSON + classOf[DummyClass].getName).setValue(encodedBytes).build

      val decoded = JsonSupport.decodeJson(classOf[DummyClass], any)
      decoded shouldBe new DummyClass("123", 321, Optional.empty())
    }

    "deserialize mandatory field with migration" in {
      val bytes = UnsafeByteOperations.unsafeWrap("""{"stringValue":"123","intValue":321}""".getBytes)
      val encodedBytes = ByteStringEncoding.encodePrimitiveBytes(bytes)
      val any =
        Any.newBuilder.setTypeUrl(JsonSupport.KALIX_JSON + classOf[DummyClass2].getName).setValue(encodedBytes).build

      val decoded = JsonSupport.decodeJson(classOf[DummyClass2], any)
      decoded shouldBe new DummyClass2("123", 321, "mandatory-value")
    }

    "deserialize renamed class" in {
      val bytes = UnsafeByteOperations.unsafeWrap("""{"stringValue":"123","intValue":321}""".getBytes)
      val encodedBytes = ByteStringEncoding.encodePrimitiveBytes(bytes)
      val any =
        Any.newBuilder.setTypeUrl(JsonSupport.KALIX_JSON + classOf[DummyClass].getName).setValue(encodedBytes).build

      val decoded = JsonSupport.decodeJson(classOf[DummyClassRenamed], any)
      decoded shouldBe new DummyClassRenamed("123", 321, Optional.empty())
    }

    "deserialize forward from DummyClass2 to DummyClass" in {
      val bytes = UnsafeByteOperations.unsafeWrap(
        """{"stringValue":"123","intValue":321,"mandatoryStringValue":"value"}""".getBytes)
      val encodedBytes = ByteStringEncoding.encodePrimitiveBytes(bytes)
      val any =
        Any.newBuilder
          .setTypeUrl(JsonSupport.KALIX_JSON + classOf[DummyClass2].getName + "#1")
          .setValue(encodedBytes)
          .build

      val decoded = JsonSupport.decodeJson(classOf[DummyClass], any)
      decoded shouldBe new DummyClass("123", 321, Optional.of("value"))
    }

    "serialize and deserialize Akka Done class" in {
      val done = Done.getInstance()
      val any = JsonSupport.encodeJson(done)
      any.getTypeUrl should ===(JsonSupport.KALIX_JSON + Done.getClass.getName)
      JsonSupport.decodeJson(classOf[Done], any) shouldBe Done.getInstance()
    }

    "serialize and deserialize a List of objects" in {

      val customers: java.util.List[MyJsonable] = new util.ArrayList[MyJsonable]()
      val foo = new MyJsonable
      foo.field = "foo"
      customers.add(foo)

      val bar = new MyJsonable
      bar.field = "bar"
      customers.add(bar)
      val any = JsonSupport.encodeJson(customers)

      val decodedCustomers =
        JsonSupport.decodeJsonCollection(classOf[MyJsonable], classOf[java.util.List[MyJsonable]], any)
      decodedCustomers.get(0).field shouldBe "foo"
      decodedCustomers.get(1).field shouldBe "bar"
    }

    "serialize JSON with an explicit type url suffix" in {
      val any = JsonSupport.encodeJson(myJsonable, "bar")
      any.getTypeUrl should ===(JsonSupport.KALIX_JSON + "bar")
    }

    "conditionally decode JSON depending on suffix" in {
      val any = JsonSupport.encodeJson(myJsonable, "bar")
      any.getTypeUrl should ===(JsonSupport.KALIX_JSON + "bar")
      JsonSupport.decodeJson(classOf[MyJsonable], "other", any).isPresent() should ===(false)
      val decoded = JsonSupport.decodeJson(classOf[MyJsonable], "bar", any)
      decoded.isPresent() should ===(true)
      decoded.get().field should ===("foo")
    }
  }

}
