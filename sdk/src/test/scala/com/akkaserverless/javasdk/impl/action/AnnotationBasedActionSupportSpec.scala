/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.action

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.javadsl.Source
import akka.stream.scaladsl.{JavaFlowSupport, Sink}
import akkaserverless.javasdk.Actionspec
import akkaserverless.javasdk.Actionspec.{In, Out}
import com.akkaserverless.javasdk.action._
import com.akkaserverless.javasdk.impl.AnySupport
import com.akkaserverless.javasdk.{Metadata, ServiceCallFactory}
import com.google.protobuf
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import java.util.Optional
import java.util.concurrent.{CompletableFuture, CompletionStage, TimeUnit}
import scala.compat.java8.FutureConverters._
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class AnnotationBasedActionSupportSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll {

  private implicit val sys = ActorSystem("AnnotationBasedActionSupportSpec")

  import sys.dispatcher

  override protected def afterAll(): Unit = {
    super.afterAll()
    sys.terminate()
  }

  private val anySupport = new AnySupport(Array(Actionspec.getDescriptor), this.getClass.getClassLoader)

  private object ctx extends ActionContext {
    override def metadata(): Metadata = Metadata.EMPTY.add("scope", "call")

    override def serviceCallFactory(): ServiceCallFactory = ???
  }

  private def create(handler: AnyRef): ActionHandler =
    new AnnotationBasedActionSupport(handler,
                                     anySupport,
                                     Actionspec.getDescriptor.findServiceByName("ActionSpecService"))

  "Annotation based action support" should {

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

      def inToOut(in: In): Out =
        Out.newBuilder().setField("out: " + in.getField).build()

      "synchronous" in test(new {
        @Handler
        def unary(in: In): Out = inToOut(in)
      })

      "synchronous JSON reply" in testJson(new {
        @Handler
        def unaryJson(in: In): JsonOut = new JsonOut(in.getField)
      })

      "asynchronous" in test(new {
        @Handler
        def unary(in: In): CompletionStage[Out] = CompletableFuture.completedFuture(inToOut(in))
      })

      "asynchronous JSON reply" in testJson(new {
        @Handler
        def unaryJson(in: In): CompletionStage[JsonOut] = CompletableFuture.completedFuture(new JsonOut(in.getField))
      })

      "in wrapped in envelope" in test(new {
        @Handler
        def unary(in: MessageEnvelope[In]): Out = {
          in.metadata().get("scope") should ===(Optional.of("message"))
          inToOut(in.payload())
        }
      })

      "synchronous out wrapped in envelope" in test(new {
        @Handler
        def unary(in: In): MessageEnvelope[Out] = MessageEnvelope.of(inToOut(in))
      })

      "asynchronous out wrapped in envelope" in test(new {
        @Handler
        def unary(in: In): CompletionStage[MessageEnvelope[Out]] =
          CompletableFuture.completedFuture(MessageEnvelope.of(inToOut(in)))
      })

      "synchronous out wrapped in reply" in test(new {
        @Handler
        def unary(in: In): ActionReply[Out] = ActionReply.message(inToOut(in))
      })

      "synchronous JSON out wrapped in reply" in testJson(new {
        @Handler
        def unaryJson(in: In): ActionReply[JsonOut] = ActionReply.message(new JsonOut(in.getField))
      })

      "asynchronous out wrapped in reply" in test(new {
        @Handler
        def unary(in: In): CompletionStage[ActionReply[Out]] =
          CompletableFuture.completedFuture(ActionReply.message(inToOut(in)))
      })

      "asynchronous JSON out wrapped in reply" in testJson(new {
        @Handler
        def unaryJson(in: In): CompletionStage[ActionReply[JsonOut]] =
          CompletableFuture.completedFuture(ActionReply.message(new JsonOut(in.getField)))
      })

      "with metadata parameter" in test(new {
        @Handler
        def unary(in: In, metadata: Metadata): Out = {
          metadata.get("scope") should ===(Optional.of("call"))
          inToOut(in)
        }
      })

      "with context parameter" in test(new {
        @Handler
        def unary(in: In, context: ActionContext): Out = inToOut(in)
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

      def inToOut(in: In): akka.stream.scaladsl.Source[Out, NotUsed] =
        akka.stream.scaladsl
          .Source(1 to 3)
          .map { idx =>
            Out.newBuilder().setField(s"out $idx: " + in.getField).build()
          }

      def inToJsonOut(in: In): akka.stream.scaladsl.Source[JsonOut, NotUsed] =
        akka.stream.scaladsl
          .Source(1 to 3)
          .map { idx =>
            new JsonOut(s"out $idx: " + in.getField)
          }

      "source" in test(new {
        @Handler
        def streamedOut(in: In): Source[Out, NotUsed] = inToOut(in).asJava
      })

      "JSON source" in testJson(new {
        @Handler
        def streamedJsonOut(in: In): Source[JsonOut, NotUsed] = inToJsonOut(in).asJava
      })

      "reactive streams publisher" in test(new {
        @Handler
        def streamedOut(in: In): org.reactivestreams.Publisher[Out] =
          inToOut(in).runWith(Sink.asPublisher(false))
      })

      "message envelope" in test(new {
        @Handler
        def streamedOut(in: MessageEnvelope[In]): Source[Out, NotUsed] = inToOut(in.payload()).asJava
      })

      "source wrapped in envelope" in test(new {
        @Handler
        def streamedOut(in: In): Source[MessageEnvelope[Out], NotUsed] =
          inToOut(in).map(MessageEnvelope.of(_)).asJava
      })

      "source wrapped in reply" in test(new {
        @Handler
        def streamedOut(in: In): Source[ActionReply[Out], NotUsed] =
          inToOut(in).map[ActionReply[Out]](ActionReply.message(_)).asJava
      })

      "with metadata parameter" in test(new {
        @Handler
        def streamedOut(in: In, metadata: Metadata): Source[Out, NotUsed] = {
          metadata.get("scope") should ===(Optional.of("call"))
          inToOut(in).asJava
        }
      })

      "with context parameter" in test(new {
        @Handler
        def streamedOut(in: In, metadata: Metadata): Source[Out, NotUsed] = inToOut(in).asJava
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

      def inToOut(in: akka.stream.scaladsl.Source[In, NotUsed]): Future[Out] =
        in.runWith(Sink.seq).map { ins =>
          Out.newBuilder().setField("out: " + ins.map(_.getField).mkString(", ")).build()
        }

      "source" in test(new {
        @Handler
        def streamedIn(in: Source[In, NotUsed]): CompletionStage[Out] = inToOut(in.asScala).toJava
      })

      "reactive streams publisher" in test(new {
        @Handler
        def streamedIn(in: org.reactivestreams.Publisher[In]): CompletionStage[Out] =
          inToOut(akka.stream.scaladsl.Source.fromPublisher(in)).toJava
      })

      "source wrapped in envelope" in test(new {
        @Handler
        def streamedIn(in: Source[MessageEnvelope[In], NotUsed]): CompletionStage[Out] =
          inToOut(in.asScala.map(_.payload)).toJava
      })

      "returns envelope" in test(new {
        @Handler
        def streamedIn(in: Source[In, NotUsed]): CompletionStage[MessageEnvelope[Out]] =
          inToOut(in.asScala).map(MessageEnvelope.of(_)).toJava
      })

      "returns reply" in test(new {
        @Handler
        def streamedIn(in: Source[In, NotUsed]): CompletionStage[ActionReply[Out]] =
          inToOut(in.asScala).map[ActionReply[Out]](ActionReply.message(_)).toJava
      })

      "with metadata parameter" in test(new {
        @Handler
        def streamedIn(in: Source[In, NotUsed], metadata: Metadata): CompletionStage[Out] = {
          metadata.get("scope") should ===(Optional.of("call"))
          inToOut(in.asScala).toJava
        }
      })

      "with context parameter" in test(new {
        @Handler
        def streamedIn(in: Source[In, NotUsed], context: ActionContext): CompletionStage[Out] =
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

      def inToOut(stream: akka.stream.scaladsl.Source[In, NotUsed]): akka.stream.scaladsl.Source[Out, NotUsed] =
        stream.map { in =>
          Out.newBuilder().setField("out: " + in.getField).build()
        }

      "source in source out" in test(new {
        @Handler
        def streamed(in: Source[In, NotUsed]): Source[Out, NotUsed] = inToOut(in.asScala).asJava
      })

      "reactive streams publisher in source out" in test(new {
        @Handler
        def streamed(in: org.reactivestreams.Publisher[In]): Source[Out, NotUsed] =
          inToOut(akka.stream.scaladsl.Source.fromPublisher(in)).asJava
      })

      "source in reactive streams publisher out" in test(new {
        @Handler
        def streamed(in: Source[In, NotUsed]): org.reactivestreams.Publisher[Out] =
          inToOut(in.asScala).runWith(Sink.asPublisher(false))
      })

      "reactive streams publisher in reactive streams publisher out" in test(new {
        @Handler
        def streamed(in: org.reactivestreams.Publisher[In]): org.reactivestreams.Publisher[Out] =
          inToOut(akka.stream.scaladsl.Source.fromPublisher(in)).runWith(Sink.asPublisher(false))
      })

      "in wrapped in envelope" in test(new {
        @Handler
        def streamed(in: Source[MessageEnvelope[In], NotUsed]): Source[Out, NotUsed] =
          inToOut(in.asScala.map(_.payload)).asJava
      })

      "out wrapped in envelope" in test(new {
        @Handler
        def streamed(in: Source[In, NotUsed]): Source[MessageEnvelope[Out], NotUsed] =
          inToOut(in.asScala).map(MessageEnvelope.of(_)).asJava
      })

      "in and out wrapped in envelope" in test(new {
        @Handler
        def streamed(in: Source[MessageEnvelope[In], NotUsed]): Source[MessageEnvelope[Out], NotUsed] =
          inToOut(in.asScala.map(_.payload())).map(MessageEnvelope.of(_)).asJava
      })

      "out wrapped in reply" in test(new {
        @Handler
        def streamed(in: Source[In, NotUsed]): Source[ActionReply[Out], NotUsed] =
          inToOut(in.asScala).map[ActionReply[Out]](ActionReply.message(_)).asJava
      })

      "in wrapped in envelope out wrapped in reply" in test(new {
        @Handler
        def streamed(in: Source[MessageEnvelope[In], NotUsed]): Source[ActionReply[Out], NotUsed] =
          inToOut(in.asScala.map(_.payload())).map[ActionReply[Out]](ActionReply.message(_)).asJava
      })

      "with metadata parameter" in test(new {
        @Handler
        def streamed(in: Source[In, NotUsed], metadata: Metadata): Source[Out, NotUsed] = {
          metadata.get("scope") should ===(Optional.of("call"))
          inToOut(in.asScala).asJava
        }
      })

      "with context parameter" in test(new {
        @Handler
        def streamed(in: Source[In, NotUsed], context: ActionContext): Source[Out, NotUsed] =
          inToOut(in.asScala).asJava
      })

    }

  }

  private def createInEnvelope(field: String) =
    MessageEnvelope.of(
      protobuf.Any.pack(In.newBuilder().setField(field).build()),
      Metadata.EMPTY.add("scope", "message")
    )

  private def assertIsOutReplyWithField(reply: ActionReply[protobuf.Any], field: String) =
    reply match {
      case message: MessageReply[protobuf.Any] =>
        val out = message.payload().unpack(classOf[Out])
        out.getField should ===(field)
      case other =>
        fail(s"$reply is not a MessageReply")
    }

  private def assertIsJsonReply(reply: ActionReply[protobuf.Any], messageValue: String) =
    reply match {
      case message: MessageReply[protobuf.Any] =>
        val out = message.payload()
        out.getTypeUrl should ===("json.akkaserverless.com/com.akkaserverless.javasdk.impl.action.JsonOut")
        val msg = AnySupport.extractBytes(out.getValue)
        msg.toStringUtf8 should ===(s"""{"message":"$messageValue"}""")
      case other =>
        fail(s"$reply is not a MessageReply")
    }
}
