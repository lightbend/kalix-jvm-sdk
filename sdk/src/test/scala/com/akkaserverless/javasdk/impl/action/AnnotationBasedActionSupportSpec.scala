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

package com.akkaserverless.javasdk.impl.action

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.javadsl.Source
import akka.stream.scaladsl.Sink
import com.akkaserverless.javasdk.action._
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.javasdk.reply.{FailureReply, MessageReply}
import com.akkaserverless.javasdk.{Metadata, Reply, ServiceCallFactory}
import com.google.protobuf
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.matchers.should.Matchers
import java.util.{Base64, Optional}
import java.util.concurrent.{CompletableFuture, CompletionStage, TimeUnit}

import scala.compat.java8.FutureConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

import com.akkaserverless.javasdk.actionspec.ActionspecApi
import com.akkaserverless.javasdk.lowlevel.ActionHandler
import com.google.protobuf.ByteString

class AnnotationBasedActionSupportSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  private implicit val sys = ActorSystem("AnnotationBasedActionSupportSpec")

  import sys.dispatcher

  override protected def afterAll(): Unit = {
    super.afterAll()
    sys.terminate()
  }

  private val anySupport = new AnySupport(Array(ActionspecApi.getDescriptor), this.getClass.getClassLoader)

  private object creationContext extends ActionCreationContext {
    override def serviceCallFactory(): ServiceCallFactory = ???
  }

  private object ctx extends ActionContext {
    override def metadata(): Metadata = Metadata.EMPTY.add("scope", "call")

    override def eventSubject(): Optional[String] =
      if (metadata().isCloudEvent)
        metadata().asCloudEvent().subject()
      else
        Optional.empty()

    override def serviceCallFactory(): ServiceCallFactory = ???
  }

  private def create(actionInstance: AnyRef): ActionHandler =
    AnnotationBasedActionSupport
      .forInstance(
        actionInstance,
        anySupport,
        ActionspecApi.getDescriptor.findServiceByName("ActionSpecService")
      )
      .create(creationContext)

  private def create(actionClass: Class[_]): ActionHandler =
    AnnotationBasedActionSupport
      .forClass(
        actionClass,
        anySupport,
        ActionspecApi.getDescriptor.findServiceByName("ActionSpecService")
      )
      .create(creationContext)

  "Annotation based action support" should {

    "construct action instances" when {

      "the constructor takes no arguments" in {
        create(classOf[NoArgConstructorTest])
      }
      "the constructor takes a context argument" in {
        create(classOf[CreationContextArgConstructorTest])
      }
      "fail if the constructor contains an unsupported parameter" in {
        a[RuntimeException] should be thrownBy create(classOf[UnsupportedConstructorParameter])
      }
      "fail if the class has more than a single constructor" in {
        a[RuntimeException] should be thrownBy create(classOf[TwoConstructors])
      }
    }

    "support invoking unary commands" when {
      def test(handler: AnyRef) = {
        val reply = create(handler)
          .handleUnary("Unary", createInEnvelope("in"), ctx)
          .toCompletableFuture
          .get(10, TimeUnit.SECONDS)
        assertIsOutReplyWithField(reply, "out: in")
      }

      def testJson(handler: AnyRef) = {
        val reply = create(handler)
          .handleUnary("UnaryJson", createInEnvelope("in"), ctx)
          .toCompletableFuture
          .get(10, TimeUnit.SECONDS)
        assertIsJsonReply(reply, "in")
      }

      def inToOut(in: ActionspecApi.In): ActionspecApi.Out =
        ActionspecApi.Out.newBuilder().setField("out: " + in.getField).build()

      def testAny(handler: AnyRef) = {
        val innerAny = protobuf.Any
          .newBuilder()
          .setTypeUrl("types.googleapis.com/wirelessmesh.FooBar")
          .setValue(
            ByteString.copyFrom(
              Base64.getDecoder.decode(
                "ChFteS10aGlyZC1sb2NhdGlvbhIIYWJjZDEyMzQaDnNvbWVAZW1haWwuY29tIgUxMTExMQ=="
              )
            )
          )
          .build()
        val outerAny = protobuf.Any
          .newBuilder()
          .setTypeUrl("types.googleapis.com/google.protobuf.Any")
          .setValue(innerAny.toByteString)
          .build()
        val reply = create(handler)
          .handleUnary(
            "UnaryAny",
            MessageEnvelope.of(
              outerAny,
              Metadata.EMPTY.add("scope", "message")
            ),
            ctx
          )
          .toCompletableFuture
          .get(10, TimeUnit.SECONDS)
        assertIsOutReplyWithField(reply, "type: types.googleapis.com/wirelessmesh.FooBar")
      }

      "synchronous" in test(new {
        @Handler
        def unary(in: ActionspecApi.In): ActionspecApi.Out = inToOut(in)
      })

      "synchronous JSON reply" in testJson(new {
        @Handler
        def unaryJson(in: ActionspecApi.In): JsonOut = new JsonOut(in.getField)
      })

      "asynchronous" in test(new {
        @Handler
        def unary(in: ActionspecApi.In): CompletionStage[ActionspecApi.Out] =
          CompletableFuture.completedFuture(inToOut(in))
      })

      "asynchronous JSON reply" in testJson(new {
        @Handler
        def unaryJson(in: ActionspecApi.In): CompletionStage[JsonOut] =
          CompletableFuture.completedFuture(new JsonOut(in.getField))
      })

      "calling catch-all method" in testAny(new {
        @Handler
        def unaryAny(in: com.google.protobuf.Any): CompletionStage[ActionspecApi.Out] =
          CompletableFuture.completedFuture(ActionspecApi.Out.newBuilder().setField("type: " + in.getTypeUrl).build())
      })

      "in wrapped in envelope" in test(new {
        @Handler
        def unary(in: MessageEnvelope[ActionspecApi.In]): ActionspecApi.Out = {
          in.metadata().get("scope") should ===(Optional.of("message"))
          inToOut(in.payload())
        }
      })

      "synchronous out wrapped in envelope" in test(new {
        @Handler
        def unary(in: ActionspecApi.In): MessageEnvelope[ActionspecApi.Out] = MessageEnvelope.of(inToOut(in))
      })

      "asynchronous out wrapped in envelope" in test(new {
        @Handler
        def unary(in: ActionspecApi.In): CompletionStage[MessageEnvelope[ActionspecApi.Out]] =
          CompletableFuture.completedFuture(MessageEnvelope.of(inToOut(in)))
      })

      "synchronous out wrapped in reply" in test(new {
        @Handler
        def unary(in: ActionspecApi.In): Reply[ActionspecApi.Out] = Reply.message(inToOut(in))
      })

      "synchronous JSON out wrapped in reply" in testJson(new {
        @Handler
        def unaryJson(in: ActionspecApi.In): Reply[JsonOut] = Reply.message(new JsonOut(in.getField))
      })

      "synchronous failure wrapped in reply" in {
        val handler = new {
          @Handler
          def unary(in: ActionspecApi.In): Reply[ActionspecApi.Out] = Reply.failure("this should blow up")
        }
        val reply = create(handler)
          .handleUnary("Unary", createInEnvelope("in"), ctx)
          .toCompletableFuture
          .get(10, TimeUnit.SECONDS)
        assertIsFailure(reply, "this should blow up")
      }

      "asynchronous out wrapped in reply" in test(new {
        @Handler
        def unary(in: ActionspecApi.In): CompletionStage[Reply[ActionspecApi.Out]] =
          CompletableFuture.completedFuture(Reply.message(inToOut(in)))
      })

      "asynchronous JSON out wrapped in reply" in testJson(new {
        @Handler
        def unaryJson(in: ActionspecApi.In): CompletionStage[Reply[JsonOut]] =
          CompletableFuture.completedFuture(Reply.message(new JsonOut(in.getField)))
      })

      "with metadata parameter" in test(new {
        @Handler
        def unary(in: ActionspecApi.In, metadata: Metadata): ActionspecApi.Out = {
          metadata.get("scope") should ===(Optional.of("call"))
          inToOut(in)
        }
      })

      "with context parameter" in test(new {
        @Handler
        def unary(in: ActionspecApi.In, context: ActionContext): ActionspecApi.Out = inToOut(in)
      })

    }

    "support invoking streamed out commands" when {
      def test(handler: AnyRef) = {
        val replies = Await.result(
          create(handler)
            .handleStreamedOut("StreamedOut", createInEnvelope("in"), ctx)
            .asScala
            .runWith(Sink.seq),
          10.seconds
        )
        replies should have size 3
        replies.zipWithIndex.foreach {
          case (reply, idx) =>
            assertIsOutReplyWithField(reply, s"out ${idx + 1}: in")
        }
      }

      def testJson(handler: AnyRef) = {
        val replies = Await.result(
          create(handler)
            .handleStreamedOut("StreamedJsonOut", createInEnvelope("in here"), ctx)
            .asScala
            .runWith(Sink.seq),
          10.seconds
        )
        replies should have size 3
        replies.zipWithIndex.foreach {
          case (reply, idx) =>
            assertIsJsonReply(reply, s"out ${idx + 1}: in here")
        }
      }

      def inToOut(in: ActionspecApi.In): akka.stream.scaladsl.Source[ActionspecApi.Out, NotUsed] =
        akka.stream.scaladsl
          .Source(1 to 3)
          .map { idx =>
            ActionspecApi.Out.newBuilder().setField(s"out $idx: " + in.getField).build()
          }

      def inToJsonOut(in: ActionspecApi.In): akka.stream.scaladsl.Source[JsonOut, NotUsed] =
        akka.stream.scaladsl
          .Source(1 to 3)
          .map { idx =>
            new JsonOut(s"out $idx: " + in.getField)
          }

      "source" in test(new {
        @Handler
        def streamedOut(in: ActionspecApi.In): Source[ActionspecApi.Out, NotUsed] = inToOut(in).asJava
      })

      "JSON source" in testJson(new {
        @Handler
        def streamedJsonOut(in: ActionspecApi.In): Source[JsonOut, NotUsed] = inToJsonOut(in).asJava
      })

      "reactive streams publisher" in test(new {
        @Handler
        def streamedOut(in: ActionspecApi.In): org.reactivestreams.Publisher[ActionspecApi.Out] =
          inToOut(in).runWith(Sink.asPublisher(false))
      })

      "message envelope" in test(new {
        @Handler
        def streamedOut(in: MessageEnvelope[ActionspecApi.In]): Source[ActionspecApi.Out, NotUsed] =
          inToOut(in.payload()).asJava
      })

      "source wrapped in envelope" in test(new {
        @Handler
        def streamedOut(in: ActionspecApi.In): Source[MessageEnvelope[ActionspecApi.Out], NotUsed] =
          inToOut(in).map(MessageEnvelope.of(_)).asJava
      })

      "source wrapped in reply" in test(new {
        @Handler
        def streamedOut(in: ActionspecApi.In): Source[Reply[ActionspecApi.Out], NotUsed] =
          inToOut(in).map[Reply[ActionspecApi.Out]](Reply.message(_)).asJava
      })

      "with metadata parameter" in test(new {
        @Handler
        def streamedOut(in: ActionspecApi.In, metadata: Metadata): Source[ActionspecApi.Out, NotUsed] = {
          metadata.get("scope") should ===(Optional.of("call"))
          inToOut(in).asJava
        }
      })

      "with context parameter" in test(new {
        @Handler
        def streamedOut(in: ActionspecApi.In, metadata: Metadata): Source[ActionspecApi.Out, NotUsed] =
          inToOut(in).asJava
      })

    }

    "support invoking streamed in commands" when {
      def test(handler: AnyRef) = {
        val reply = create(handler)
          .handleStreamedIn(
            "StreamedIn",
            akka.stream.scaladsl
              .Source(1 to 3)
              .map(idx => createInEnvelope("in " + idx))
              .asJava,
            ctx
          )
          .toCompletableFuture
          .get(10, TimeUnit.SECONDS)

        assertIsOutReplyWithField(reply, "out: in 1, in 2, in 3")
      }

      def inToOut(in: akka.stream.scaladsl.Source[ActionspecApi.In, NotUsed]): Future[ActionspecApi.Out] =
        in.runWith(Sink.seq).map { ins =>
          ActionspecApi.Out.newBuilder().setField("out: " + ins.map(_.getField).mkString(", ")).build()
        }

      "source" in test(new {
        @Handler
        def streamedIn(in: Source[ActionspecApi.In, NotUsed]): CompletionStage[ActionspecApi.Out] =
          inToOut(in.asScala).toJava
      })

      "reactive streams publisher" in test(new {
        @Handler
        def streamedIn(in: org.reactivestreams.Publisher[ActionspecApi.In]): CompletionStage[ActionspecApi.Out] =
          inToOut(akka.stream.scaladsl.Source.fromPublisher(in)).toJava
      })

      "source wrapped in envelope" in test(new {
        @Handler
        def streamedIn(in: Source[MessageEnvelope[ActionspecApi.In], NotUsed]): CompletionStage[ActionspecApi.Out] =
          inToOut(in.asScala.map(_.payload)).toJava
      })

      "returns envelope" in test(new {
        @Handler
        def streamedIn(in: Source[ActionspecApi.In, NotUsed]): CompletionStage[MessageEnvelope[ActionspecApi.Out]] =
          inToOut(in.asScala).map(MessageEnvelope.of(_)).toJava
      })

      "returns reply" in test(new {
        @Handler
        def streamedIn(in: Source[ActionspecApi.In, NotUsed]): CompletionStage[Reply[ActionspecApi.Out]] =
          inToOut(in.asScala).map[Reply[ActionspecApi.Out]](Reply.message(_)).toJava
      })

      "with metadata parameter" in test(new {
        @Handler
        def streamedIn(in: Source[ActionspecApi.In, NotUsed],
                       metadata: Metadata): CompletionStage[ActionspecApi.Out] = {
          metadata.get("scope") should ===(Optional.of("call"))
          inToOut(in.asScala).toJava
        }
      })

      "with context parameter" in test(new {
        @Handler
        def streamedIn(in: Source[ActionspecApi.In, NotUsed],
                       context: ActionContext): CompletionStage[ActionspecApi.Out] =
          inToOut(in.asScala).toJava
      })

    }

    "support invoking streamed commands" when {
      def test(handler: AnyRef) = {
        val replies = Await.result(
          create(handler)
            .handleStreamed(
              "Streamed",
              akka.stream.scaladsl
                .Source(1 to 3)
                .map(idx => createInEnvelope("in " + idx))
                .asJava,
              ctx
            )
            .asScala
            .runWith(Sink.seq),
          10.seconds
        )

        replies should have size 3
        replies.zipWithIndex.foreach {
          case (reply, idx) =>
            assertIsOutReplyWithField(reply, s"out: in ${idx + 1}")
        }
      }

      def inToOut(
          stream: akka.stream.scaladsl.Source[ActionspecApi.In, NotUsed]
      ): akka.stream.scaladsl.Source[ActionspecApi.Out, NotUsed] =
        stream.map { in =>
          ActionspecApi.Out.newBuilder().setField("out: " + in.getField).build()
        }

      "source in source out" in test(new {
        @Handler
        def streamed(in: Source[ActionspecApi.In, NotUsed]): Source[ActionspecApi.Out, NotUsed] =
          inToOut(in.asScala).asJava
      })

      "reactive streams publisher in source out" in test(new {
        @Handler
        def streamed(in: org.reactivestreams.Publisher[ActionspecApi.In]): Source[ActionspecApi.Out, NotUsed] =
          inToOut(akka.stream.scaladsl.Source.fromPublisher(in)).asJava
      })

      "source in reactive streams publisher out" in test(new {
        @Handler
        def streamed(in: Source[ActionspecApi.In, NotUsed]): org.reactivestreams.Publisher[ActionspecApi.Out] =
          inToOut(in.asScala).runWith(Sink.asPublisher(false))
      })

      "reactive streams publisher in reactive streams publisher out" in test(new {
        @Handler
        def streamed(
            in: org.reactivestreams.Publisher[ActionspecApi.In]
        ): org.reactivestreams.Publisher[ActionspecApi.Out] =
          inToOut(akka.stream.scaladsl.Source.fromPublisher(in)).runWith(Sink.asPublisher(false))
      })

      "in wrapped in envelope" in test(new {
        @Handler
        def streamed(in: Source[MessageEnvelope[ActionspecApi.In], NotUsed]): Source[ActionspecApi.Out, NotUsed] =
          inToOut(in.asScala.map(_.payload)).asJava
      })

      "out wrapped in envelope" in test(new {
        @Handler
        def streamed(in: Source[ActionspecApi.In, NotUsed]): Source[MessageEnvelope[ActionspecApi.Out], NotUsed] =
          inToOut(in.asScala).map(MessageEnvelope.of(_)).asJava
      })

      "in and out wrapped in envelope" in test(new {
        @Handler
        def streamed(
            in: Source[MessageEnvelope[ActionspecApi.In], NotUsed]
        ): Source[MessageEnvelope[ActionspecApi.Out], NotUsed] =
          inToOut(in.asScala.map(_.payload())).map(MessageEnvelope.of(_)).asJava
      })

      "out wrapped in reply" in test(new {
        @Handler
        def streamed(in: Source[ActionspecApi.In, NotUsed]): Source[Reply[ActionspecApi.Out], NotUsed] =
          inToOut(in.asScala).map[Reply[ActionspecApi.Out]](Reply.message(_)).asJava
      })

      "in wrapped in envelope out wrapped in reply" in test(new {
        @Handler
        def streamed(
            in: Source[MessageEnvelope[ActionspecApi.In], NotUsed]
        ): Source[Reply[ActionspecApi.Out], NotUsed] =
          inToOut(in.asScala.map(_.payload())).map[Reply[ActionspecApi.Out]](Reply.message(_)).asJava
      })

      "with metadata parameter" in test(new {
        @Handler
        def streamed(in: Source[ActionspecApi.In, NotUsed], metadata: Metadata): Source[ActionspecApi.Out, NotUsed] = {
          metadata.get("scope") should ===(Optional.of("call"))
          inToOut(in.asScala).asJava
        }
      })

      "with context parameter" in test(new {
        @Handler
        def streamed(in: Source[ActionspecApi.In, NotUsed],
                     context: ActionContext): Source[ActionspecApi.Out, NotUsed] =
          inToOut(in.asScala).asJava
      })

    }

  }

  private def createInEnvelope(field: String) =
    MessageEnvelope.of(
      protobuf.Any.pack(ActionspecApi.In.newBuilder().setField(field).build()),
      Metadata.EMPTY.add("scope", "message")
    )

  private def assertIsOutReplyWithField(reply: Reply[protobuf.Any], field: String) =
    reply match {
      case message: MessageReply[protobuf.Any] =>
        val out = message.payload().unpack(classOf[ActionspecApi.Out])
        out.getField should ===(field)
      case other =>
        fail(s"$reply is not a MessageReply")
    }

  private def assertIsJsonReply(reply: Reply[protobuf.Any], messageValue: String) =
    reply match {
      case message: MessageReply[protobuf.Any] =>
        val out = message.payload()
        out.getTypeUrl should ===("json.akkaserverless.com/com.akkaserverless.javasdk.impl.action.JsonOut")
        val msg = AnySupport.extractBytes(out.getValue)
        msg.toStringUtf8 should ===(s"""{"message":"$messageValue"}""")
      case other =>
        fail(s"$reply is not a MessageReply")
    }

  private def assertIsFailure(reply: Reply[protobuf.Any], failureDescription: String) =
    reply match {
      case message: FailureReply[protobuf.Any] =>
        message.description() should ===(failureDescription)
      case other =>
        fail(s"$reply is not a FailureReply")
    }
}

@Action
private class NoArgConstructorTest() {}

@Action
private class CreationContextArgConstructorTest(context: ActionCreationContext) {
  context should not be null
}

@Action
private class UnsupportedConstructorParameter(foo: String)

@Action
private class TwoConstructors(foo: String) {
  def this() = this("message")
}
