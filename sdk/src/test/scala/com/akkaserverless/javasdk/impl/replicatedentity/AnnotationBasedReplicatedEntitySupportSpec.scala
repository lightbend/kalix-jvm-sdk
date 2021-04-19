/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.replicatedentity

import com.akkaserverless.javasdk._
import com.akkaserverless.javasdk.replicatedentity._
import com.akkaserverless.javasdk.impl.{AnySupport, ResolvedServiceMethod, ResolvedType}
import com.example.shoppingcart.ShoppingCart
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{ByteString, Any => JavaPbAny}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import java.util.Optional

class AnnotationBasedReplicatedEntitySupportSpec extends AnyWordSpec with Matchers {

  trait BaseContext extends Context {
    override def serviceCallFactory(): ServiceCallFactory = new ServiceCallFactory {
      override def lookup[T](serviceName: String, methodName: String, messageType: Class[T]): ServiceCallRef[T] =
        throw new NoSuchElementException
    }
  }

  trait ReplicatedEntityFactoryContext extends AbstractReplicatedEntityFactory {
    override protected def anySupport: AnySupport = AnnotationBasedReplicatedEntitySupportSpec.this.anySupport
    override protected def newEntity[C <: InternalReplicatedData](entity: C): C = entity
  }

  val anySupport = new AnySupport(Array(ShoppingCart.getDescriptor), this.getClass.getClassLoader)

  object MockCreationContext extends MockCreationContext(None)
  class MockCreationContext(replicatedData: Option[ReplicatedData] = None)
      extends ReplicatedEntityCreationContext
      with BaseContext
      with ReplicatedEntityFactoryContext {
    override def entityId(): String = "foo"
    override def state[T <: ReplicatedData](dataType: Class[T]): Optional[T] = replicatedData match {
      case Some(data) if dataType.isInstance(data) => Optional.of(dataType.cast(data))
      case None => Optional.empty()
      case Some(wrongType) =>
        throw new IllegalStateException(
          s"The current ${wrongType} replicated data doesn't match requested type of ${dataType.getSimpleName}"
        )
    }
    override def getWriteConsistency: WriteConsistency = WriteConsistency.LOCAL
    override def setWriteConsistency(writeConsistency: WriteConsistency): Unit = ()
  }

  object WrappedResolvedType extends ResolvedType[Wrapped] {
    override def typeClass: Class[Wrapped] = classOf[Wrapped]
    override def typeUrl: String = AnySupport.DefaultTypeUrlPrefix + "/wrapped"
    override def parseFrom(bytes: ByteString): Wrapped = Wrapped(bytes.toStringUtf8)
    override def toByteString(value: Wrapped): ByteString = ByteString.copyFromUtf8(value.value)
  }

  object StringResolvedType extends ResolvedType[String] {
    override def typeClass: Class[String] = classOf[String]
    override def typeUrl: String = AnySupport.DefaultTypeUrlPrefix + "/string"
    override def parseFrom(bytes: ByteString): String = bytes.toStringUtf8
    override def toByteString(value: String): ByteString = ByteString.copyFromUtf8(value)
  }

  case class Wrapped(value: String)
  val serviceDescriptor = ShoppingCart.getDescriptor.findServiceByName("ShoppingCartService")
  val descriptor = serviceDescriptor.findMethodByName("AddItem")
  val method = ResolvedServiceMethod(descriptor, StringResolvedType, WrappedResolvedType)

  def create(behavior: AnyRef, methods: ResolvedServiceMethod[_, _]*) =
    new AnnotationBasedReplicatedEntitySupport(behavior.getClass,
                                               anySupport,
                                               methods.map(m => m.descriptor.getName -> m).toMap,
                                               Some(_ => behavior)).create(new MockCreationContext())

  def create(clazz: Class[_], replicatedData: Option[ReplicatedData] = None) =
    new AnnotationBasedReplicatedEntitySupport(clazz, anySupport, Map.empty, None)
      .create(new MockCreationContext(replicatedData))

  def command(str: String) =
    ScalaPbAny.toJavaProto(ScalaPbAny(StringResolvedType.typeUrl, StringResolvedType.toByteString(str)))

  def decodeWrapped(any: JavaPbAny) = {
    any.getTypeUrl should ===(WrappedResolvedType.typeUrl)
    WrappedResolvedType.parseFrom(any.getValue)
  }

  "Event sourced annotation support" should {
    "support entity construction" when {

      "there is an optional replicated entity constructor and the entity is empty" in {
        create(classOf[OptionalEmptyEntityConstructorTest], None)
      }

      "there is an optional replicated entity constructor and the entity is non empty" in {
        create(classOf[OptionalEntityConstructorTest], Some(MockCreationContext.newVote()))
      }

      "there is an optional replicated entity constructor and the replicated data has the wrong type" in {
        an[IllegalStateException] should be thrownBy create(classOf[OptionalEntityConstructorTest],
                                                            Some(MockCreationContext.newGCounter()))
      }

      "there is a replicated entity constructor and the entity is non empty" in {
        create(classOf[ReplicatedEntityConstructorTest], Some(MockCreationContext.newVote()))
      }

      "there is a replicated entity constructor and the entity is empty" in {
        create(classOf[ReplicatedEntityConstructorTest], None)
      }

      "there is a replicated entity constructor and the relicated data has the wrong type" in {
        an[IllegalStateException] should be thrownBy create(classOf[ReplicatedEntityConstructorTest],
                                                            Some(MockCreationContext.newGCounter()))
      }

      "there is a provided entity factory" in {
        val factory = new EntityFactory {
          override def create(context: EntityContext): AnyRef = new ReplicatedEntityFactoryTest(context)
          override def entityClass: Class[_] = classOf[ReplicatedEntityFactoryTest]
        }
        val support = new AnnotationBasedReplicatedEntitySupport(factory, anySupport, serviceDescriptor)
        support.create(MockCreationContext)
      }

    }

    // TODO: replicated entity command handlers should be tested
  }
}

import org.scalatest.matchers.should.Matchers._

@ReplicatedEntity
private class OptionalEmptyEntityConstructorTest(replicatedData: Optional[Vote]) {
  replicatedData should ===(Optional.empty())
}

@ReplicatedEntity
private class OptionalEntityConstructorTest(replicatedData: Optional[Vote]) {
  replicatedData.isPresent shouldBe true
  replicatedData.get shouldBe a[Vote]
}

@ReplicatedEntity
private class ReplicatedEntityConstructorTest(replicatedData: Vote) {
  replicatedData shouldBe a[Vote]
}

@ReplicatedEntity
private class ReplicatedEntityFactoryTest(ctx: EntityContext) {
  ctx shouldBe a[ReplicatedEntityCreationContext]
  ctx.entityId should ===("foo")
}
