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

package com.akkaserverless.javasdk.impl.eventsourcedentity

import com.akkaserverless.javasdk.eventsourcedentity._
import com.akkaserverless.javasdk.impl.{AnySupport, ResolvedServiceMethod, ResolvedType}
import com.akkaserverless.javasdk._
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase.Effect
import com.akkaserverless.javasdk.impl.effect.ErrorReplyImpl
import com.akkaserverless.javasdk.impl.effect.SecondaryEffectImpl
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityEffectImpl.EmitEvents
import com.akkaserverless.javasdk.impl.effect.MessageReplyImpl
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl.PrimaryEffectImpl
import com.akkaserverless.javasdk.reply.ErrorReply
import com.example.shoppingcart.ShoppingCartApi
import com.google.protobuf
import com.google.protobuf.any.{Any => ScalaPbAny}
import com.google.protobuf.{ByteString, Any => JavaPbAny}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

class AnnotationBasedEventSourcedSupportSpec extends AnyWordSpec with Matchers {

  trait BaseContext extends Context {
    override def serviceCallFactory(): ServiceCallFactory = new ServiceCallFactory {
      override def lookup[T](serviceName: String, methodName: String, messageType: Class[T]): ServiceCallRef[T] =
        throw new NoSuchElementException
    }
  }

  object MockContext extends EventSourcedContext with BaseContext {
    override def entityId(): String = "foo"
  }

  class MockCommandContext extends CommandContext with BaseContext {
    override def sequenceNumber(): Long = 10
    override def commandName(): String = "AddItem"
    override def commandId(): Long = 20
    override def entityId(): String = "foo"
    override def fail(errorMessage: String): RuntimeException = ???
    override def forward(to: ServiceCall): Unit = ???
    override def effect(effect: ServiceCall, synchronous: Boolean): Unit = ???
    override def metadata(): Metadata = Metadata.EMPTY
  }

  val eventCtx = new EventContext with BaseContext {
    override def sequenceNumber(): Long = 10
    override def entityId(): String = "foo"
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
  val anySupport = new AnySupport(Array(ShoppingCartApi.getDescriptor), this.getClass.getClassLoader)
  val serviceDescriptor = ShoppingCartApi.getDescriptor.findServiceByName("ShoppingCartService")
  val descriptor = serviceDescriptor.findMethodByName("AddItem")
  val method = ResolvedServiceMethod(descriptor, StringResolvedType, WrappedResolvedType)
  val eventContextFactory = (sequenceNr: Long) =>
    new EventContext {
      override def sequenceNumber(): Long = sequenceNr
      override def entityId(): String = ???
      override def serviceCallFactory(): ServiceCallFactory = ???
    }

  def create(behavior: EventSourcedEntityBase[_], methods: ResolvedServiceMethod[_, _]*) =
    new AnnotationBasedEventSourcedSupport(behavior.getClass,
                                           anySupport,
                                           methods.map(m => m.descriptor.getName -> m).toMap,
                                           Some(_ => behavior)).create(MockContext)

  def create(clazz: Class[_]) =
    new AnnotationBasedEventSourcedSupport(clazz, anySupport, Map.empty, None).create(MockContext)

  def command(str: String) =
    ScalaPbAny.toJavaProto(ScalaPbAny(StringResolvedType.typeUrl, StringResolvedType.toByteString(str)))

  def decodeWrapped(reply: Reply[JavaPbAny]): Wrapped =
    reply match {
      case com.akkaserverless.javasdk.impl.reply.MessageReplyImpl(any, _, _) =>
        decodeWrapped(any)
    }

  def decodeWrapped(any: JavaPbAny) = {
    any.getTypeUrl should ===(WrappedResolvedType.typeUrl)
    WrappedResolvedType.parseFrom(any.getValue)
  }

  def decodeWrapped(effect: Effect[JavaPbAny]): Wrapped = {
    effect.asInstanceOf[EventSourcedEntityEffectImpl[JavaPbAny]].primaryEffect match {
      case EmitEvents(events) =>
        val list = events.toList
        if (list.size != 1) fail("Got multiple or zero events back")
        else {
          list.head match {
            case any: String =>
              // FIXME should it rather be serialized when we get here?
              Wrapped(any)
            case any: JavaPbAny =>
              any.getTypeUrl should ===(WrappedResolvedType.typeUrl)
              WrappedResolvedType.parseFrom(any.getValue)
          }
        }

    }
  }
  def reply(secondaryEffectImpl: SecondaryEffectImpl): Any = secondaryEffectImpl match {
    case MessageReplyImpl(reply, _, _) => reply
    case other => fail()
  }

  class TestEntityBase extends EventSourcedEntityBase[JavaPbAny] {
    override protected def emptyState(): JavaPbAny = JavaPbAny.getDefaultInstance
  }

  "Event sourced annotation support" should {
    "support entity construction" when {

      "there is a noarg constructor" in {
        create(classOf[NoArgConstructorTest])
      }

      "there is a constructor with an EntityId annotated parameter" in {
        create(classOf[EntityIdArgConstructorTest])
      }

      "there is a constructor with a EventSourcedEntityCreationContext parameter" in {
        create(classOf[CreationContextArgConstructorTest])
      }

      "there is a constructor with multiple parameters" in {
        create(classOf[MultiArgConstructorTest])
      }

      "fail if the constructor contains an unsupported parameter" in {
        a[RuntimeException] should be thrownBy create(classOf[UnsupportedConstructorParameter])
      }

      "there is a provided entity factory" in {
        val factory = new EntityFactory {
          override def create(context: EntityContext): EventSourcedEntityBase[_] = new FactoryCreatedEntityTest(context)
          override def entityClass: Class[_] = classOf[FactoryCreatedEntityTest]
        }
        val eventSourcedSupport = new AnnotationBasedEventSourcedSupport(factory, anySupport, serviceDescriptor)
        val handler = eventSourcedSupport.create(MockContext)
        handler.handleEvent("my-event", eventCtx)
      }

    }

    "support event handlers" when {
      "handle events of a subclass" in {
        var invoked = false
        val handler = create(new TestEntityBase {
          @EventHandler
          def handle(state: Any, event: AnyRef): Any = {
            event should ===("my-event")
            invoked = true
            state
          }
        })
        handler.handleEvent("my-event", eventCtx)
        invoked shouldBe true
      }

      "fail if there's a bad context type" in {
        a[RuntimeException] should be thrownBy create(new TestEntityBase {
          @EventHandler
          def handle(state: Any, event: String, ctx: CommandContext): Any = state
        })
      }

      "fail if the event handler class conflicts with the event class" in {
        a[RuntimeException] should be thrownBy create(new TestEntityBase {
          @EventHandler(eventClass = classOf[Integer])
          def handle(state: Any, event: String): Any = state
        })
      }

      "fail if there are two event handlers for the same type" in {
        a[RuntimeException] should be thrownBy create(new TestEntityBase {
          @EventHandler
          def handle1(state: Any, event: String): Any = state

          @EventHandler
          def handle2(state: Any, event: String): Any = state
        })
      }

    }

    "support command handlers" when {

      "allow emitting events" in {
        val handler = create(
          new TestEntityBase {
            @CommandHandler
            def addItem(state: Any, msg: String): Effect[Wrapped] = {
              commandContext().commandName() should ===("AddItem")
              effects().emitEvent(msg + " event").thenReply(_ => Wrapped(msg))
            }
            @EventHandler
            def string(state: Any, event: String) = state
          },
          method
        )
        val ctx = new MockCommandContext
        val result = handler.handleCommand("AddItem", command("blah"), ctx, eventContextFactory)
        result.events should have size (1)
        result.events.head should ===("blah event")
        reply(result.secondaryEffect) should equal(Wrapped("blah"))
      }

      "fail if there's two command handlers for the same command" in {
        a[RuntimeException] should be thrownBy create(
          new TestEntityBase {
            @CommandHandler
            def addItem(state: Any, msg: String): Effect[Wrapped] =
              effects.reply(Wrapped(msg))
            @CommandHandler
            def addItem(state: Any, msg: String, something: Any): Effect[Wrapped] =
              effects.reply(Wrapped(msg))
          },
          method
        )
      }

      "fail if there's no command with that name" in {
        a[RuntimeException] should be thrownBy create(new TestEntityBase {
          @CommandHandler
          def wrongName(state: Any, msg: String): Effect[Wrapped] =
            effects().reply(Wrapped(msg))
        }, method)
      }

      "fail if there's a replicated entity command handler" in {
        val ex = the[RuntimeException] thrownBy create(
            new TestEntityBase {
              @com.akkaserverless.javasdk.replicatedentity.CommandHandler
              def addItem(state: Any, msg: String): Effect[Wrapped] =
                effects.reply(Wrapped(msg))
            },
            method
          )
        ex.getMessage should include("Did you mean")
        ex.getMessage should include(classOf[CommandHandler].getName)
      }

      "unwrap exceptions" in {
        val handler = create(new TestEntityBase {
          @CommandHandler
          def addItem(state: Any, command: Any): Effect[Wrapped] = throw new RuntimeException("foo")
        }, method)
        val ex = the[RuntimeException] thrownBy handler.handleCommand("AddItem",
                                                                      command("nothing"),
                                                                      new MockCommandContext,
                                                                      eventContextFactory)
        ex.getStackTrace()(0)
          .toString should include regex """.*AnnotationBasedEventSourcedSupportSpec.*addItem.*AnnotationBasedEventSourcedSupportSpec\.scala:\d+"""
        ex.toString should ===("java.lang.RuntimeException: foo")
      }

      "receive Failure Reply" in {
        val handler = create(new TestEntityBase {
          @CommandHandler
          def addItem(state: Any, command: Any): Effect[Wrapped] = effects.error("foo")
        }, method)
        val result = handler.handleCommand("AddItem", command("nothing"), new MockCommandContext, eventContextFactory)
        assertIsFailure(result.secondaryEffect, "foo")
      }

    }
  }

  private def assertIsFailure(result: SecondaryEffectImpl, expectedDesc: String): Unit = {
    result match {
      case ErrorReplyImpl(desc, _) =>
        desc should ===(expectedDesc)
      case other => fail(s"$other is not a FailureReply")
    }
  }

  private def assertIsFailure(reply: Reply[protobuf.Any], failureDescription: String) =
    reply match {
      case message: ErrorReply[protobuf.Any] =>
        message.description() should ===(failureDescription)
      case other =>
        fail(s"$reply is not a FailureReply")
    }
  private def assertIsFailure(effect: Effect[protobuf.Any], failureDescription: String) =
    effect.asInstanceOf[EventSourcedEntityEffectImpl[JavaPbAny]].secondaryEffect(null) match {
      case message: ErrorReplyImpl[protobuf.Any @unchecked] =>
        message.description should ===(failureDescription)
      case other =>
        fail(s"$effect is not a Failure")
    }
}

import org.scalatest.matchers.should.Matchers._

@EventSourcedEntity(entityType = "NoArgConstructorTest")
private class NoArgConstructorTest() extends EventSourcedEntityBase[Unit] {
  override def emptyState(): Unit = ()
}

@EventSourcedEntity(entityType = "EntityIdArgConstructorTest")
private class EntityIdArgConstructorTest(@EntityId entityId: String) extends EventSourcedEntityBase[Unit] {
  override def emptyState(): Unit = ()
  entityId should ===("foo")
}

@EventSourcedEntity(entityType = "CreationContextArgConstructorTest")
private class CreationContextArgConstructorTest(ctx: EventSourcedEntityCreationContext)
    extends EventSourcedEntityBase[Unit] {
  override def emptyState(): Unit = ()
  ctx.entityId should ===("foo")
}

@EventSourcedEntity(entityType = "MultiArgConstructorTest")
private class MultiArgConstructorTest(ctx: EventSourcedContext, @EntityId entityId: String)
    extends EventSourcedEntityBase[Unit] {
  override def emptyState(): Unit = ()
  ctx.entityId should ===("foo")
  entityId should ===("foo")
}

@EventSourcedEntity(entityType = "UnsupportedConstructorParameter")
private class UnsupportedConstructorParameter(foo: String) extends EventSourcedEntityBase[Unit] {
  override def emptyState(): Unit = ()
}

private class FactoryCreatedEntityTest(ctx: EntityContext) extends EventSourcedEntityBase[String] {
  ctx.entityId should ===("foo")

  override def emptyState(): String = ""

  @EventHandler
  def handle(currentState: String, event: Any): String = {
    event should ===("my-event")
    currentState
  }

}
