/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.action

import akka.NotUsed
import akka.actor.testkit.typed.scaladsl.LogCapturing
import akka.actor.testkit.typed.scaladsl.LoggingTestKit
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.adapter._
import akka.stream.javadsl.Source
import akka.stream.scaladsl.Sink
import kalix.javasdk.action.Action
import kalix.javasdk.action.MessageEnvelope
import kalix.javasdk.actionspec.ActionspecApi
import kalix.javasdk.impl.ActionFactory
import kalix.javasdk.impl.AnySupport
import kalix.javasdk.impl.GrpcDeferredCall
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.impl.ProxyInfoHolder
import kalix.javasdk.impl.ResolvedServiceMethod
import kalix.javasdk.impl.effect.SideEffectImpl
import kalix.protocol.action.ActionCommand
import kalix.protocol.action.ActionResponse
import kalix.protocol.action.Actions
import kalix.protocol.component.Reply
import com.google.protobuf
import com.google.protobuf.any.{ Any => ScalaPbAny }
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Inside
import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

class ActionHandlerSpec
    extends ScalaTestWithActorTestKit
    with LogCapturing
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with Inside
    with OptionValues {

  import testKit.internalSystem.executionContext
  private val classicSystem = system.toClassic

  private val serviceDescriptor =
    ActionspecApi.getDescriptor.findServiceByName("ActionSpecService")
  private val serviceName = serviceDescriptor.getFullName
  private val anySupport = new AnySupport(Array(ActionspecApi.getDescriptor), this.getClass.getClassLoader)

  def create(handler: ActionRouter[_]): Actions = {
    val actionFactory: ActionFactory = _ => handler
    val service = new ActionService(actionFactory, serviceDescriptor, Array(), anySupport, None)

    val services = Map(serviceName -> service)

    //setting tracing as disabled, emulating that is discovered from the proxy.
    ProxyInfoHolder(system).overrideTracingCollectorEndpoint("")

    new ActionsImpl(classicSystem, services)
  }

  "The action service" should {
    "invoke unary commands" in {
      val service = create(new AbstractHandler {

        override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[Any] =
          createReplyEffect("out: " + extractInField(message))
      })

      val reply =
        Await.result(service.handleUnary(ActionCommand(serviceName, "Unary", createInPayload("in"))), 10.seconds)

      inside(reply.response) { case ActionResponse.Response.Reply(Reply(payload, _, _)) =>
        extractOutField(payload) should ===("out: in")
      }
    }

    "turn thrown unary command handler exceptions into failure responses" in {
      val service = create(new AbstractHandler {

        override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[Any] =
          throw new RuntimeException("boom")
      })

      val reply =
        LoggingTestKit
          .error("Failure during handling of command")
          .expect {
            Await.result(service.handleUnary(ActionCommand(serviceName, "Unary", createInPayload("in"))), 10.seconds)
          }

      inside(reply.response) { case ActionResponse.Response.Failure(fail) =>
        fail.description should startWith("Unexpected error")
      }
    }

    "invoke streamed in commands" in {
      val service = create(new AbstractHandler {
        override def handleStreamedIn(
            commandName: String,
            stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[Any] =
          createAsyncReplyEffect(
            stream.asScala
              .map(extractInField)
              .runWith(Sink.seq)
              .map(ins => createReplyEffect("out: " + ins.mkString(", "))))
      })

      val reply = Await.result(
        service.handleStreamedIn(
          akka.stream.scaladsl.Source
            .single(ActionCommand(serviceName, "StreamedIn"))
            .concat(
              akka.stream.scaladsl.Source(1 to 3).map(idx => ActionCommand(payload = createInPayload(s"in $idx"))))),
        10.seconds)

      inside(reply.response) { case ActionResponse.Response.Reply(Reply(payload, _, _)) =>
        extractOutField(payload) should ===("out: in 1, in 2, in 3")
      }
    }

    "invoke streamed out commands" in {
      val service = create(new AbstractHandler {
        override def handleStreamedOut(
            commandName: String,
            message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = {
          val in = extractInField(message)
          akka.stream.scaladsl
            .Source(1 to 3)
            .asJava
            .map(idx => createReplyEffect(s"out $idx: $in"))
            .asInstanceOf[Source[Action.Effect[_], NotUsed]]
        }
      })

      val replies = Await.result(
        service
          .handleStreamedOut(ActionCommand(serviceName, "Unary", createInPayload("in")))
          .runWith(Sink.seq),
        10.seconds)

      replies.zipWithIndex.foreach { case (reply, idx) =>
        inside(reply.response) { case ActionResponse.Response.Reply(Reply(payload, _, _)) =>
          extractOutField(payload) should ===(s"out ${idx + 1}: in")
        }
      }
    }

    "turn streamed out command handler throwing exceptions into failure responses" in {
      val service = create(new AbstractHandler {
        override def handleStreamedOut(
            commandName: String,
            message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = {
          throw new RuntimeException("boom")
        }
      })

      val replies =
        LoggingTestKit.error("Failure during handling of command").expect {
          Await.result(
            service
              .handleStreamedOut(ActionCommand(serviceName, "Unary", createInPayload("in")))
              .runWith(Sink.seq),
            10.seconds)
        }

      replies should have size 1
      inside(replies.head) { case ActionResponse(ActionResponse.Response.Failure(fail), _, _) =>
        fail.description should startWith("Unexpected error")
      }
    }

    "turn streamed out command handler failed stream into failure responses" in {
      val service = create(new AbstractHandler {
        override def handleStreamedOut(
            commandName: String,
            message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = {
          Source.failed(new RuntimeException("boom"))
        }
      })

      val replies =
        LoggingTestKit.error("Failure during handling of command").expect {
          Await.result(
            service
              .handleStreamedOut(ActionCommand(serviceName, "Unary", createInPayload("in")))
              .runWith(Sink.seq),
            10.seconds)
        }

      replies should have size 1
      inside(replies.head) { case ActionResponse(ActionResponse.Response.Failure(fail), _, _) =>
        fail.description should startWith("Unexpected error")
      }
    }

    "invoke streamed commands" in {
      val service = create(new AbstractHandler {
        override def handleStreamed(
            commandName: String,
            stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed] =
          stream.asScala
            .map(extractInField)
            .map(in => createReplyEffect(s"out: $in"))
            .asJava
            .asInstanceOf[Source[Action.Effect[_], NotUsed]]
      })

      val replies = Await.result(
        service
          .handleStreamed(
            akka.stream.scaladsl.Source
              .single(ActionCommand(serviceName, "StreamedIn"))
              .concat(
                akka.stream.scaladsl.Source(1 to 3).map(idx => ActionCommand(payload = createInPayload(s"in $idx")))))
          .runWith(Sink.seq),
        10.seconds)

      replies.zipWithIndex.foreach { case (reply, idx) =>
        inside(reply.response) { case ActionResponse.Response.Reply(Reply(payload, _, _)) =>
          extractOutField(payload) should ===(s"out: in ${idx + 1}")
        }
      }
    }

    "pass over side effects from an outer async effect to the inner one" in {
      val dummyResolvedMethod = ResolvedServiceMethod(
        serviceDescriptor.getMethods.get(0),
        anySupport.resolveTypeDescriptor(serviceDescriptor.getMethods.get(0).getInputType),
        anySupport.resolveTypeDescriptor(serviceDescriptor.getMethods.get(0).getOutputType))

      val service = create(new AbstractHandler {

        override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[Any] = {
          createAsyncReplyEffect(Future {
            createReplyEffect("reply").addSideEffect(
              SideEffectImpl(
                // Note that this is never constructed manually/dynamically in actual use
                // but only by code generated based on the descriptors
                GrpcDeferredCall(
                  message.payload(),
                  MetadataImpl.Empty,
                  serviceName,
                  dummyResolvedMethod.method().getName,
                  _ => ???),
                false))
          }).addSideEffect(
            SideEffectImpl(
              GrpcDeferredCall(
                message.payload(),
                MetadataImpl.Empty,
                serviceName,
                dummyResolvedMethod.descriptor.getName,
                _ => ???),
              true))
        }
      })

      val reply =
        Await.result(service.handleUnary(ActionCommand(serviceName, "Unary", createInPayload("in"))), 10.seconds)

      reply match {
        case ActionResponse(_, sideEffects, _) =>
          sideEffects should have size 2
      }

    }

    "allow async ignore" in {
      val dummyResolvedMethod = ResolvedServiceMethod(
        serviceDescriptor.getMethods.get(0),
        anySupport.resolveTypeDescriptor(serviceDescriptor.getMethods.get(0).getInputType),
        anySupport.resolveTypeDescriptor(serviceDescriptor.getMethods.get(0).getOutputType))

      val service = create(new AbstractHandler {

        override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[Any] = {
          createAsyncReplyEffect(Future.successful(createIgnoreEffect())).addSideEffect(
            // will be ignored
            SideEffectImpl(
              GrpcDeferredCall(
                message.payload(),
                MetadataImpl.Empty,
                serviceName,
                dummyResolvedMethod.descriptor.getName,
                _ => ???),
              true))
        }
      })

      val reply =
        Await.result(service.handleUnary(ActionCommand(serviceName, "Unary", createInPayload("in"))), 10.seconds)

      reply match {
        case ActionResponse(ActionResponse.Response.Empty, sideEffects, _) =>
          sideEffects should have size 0
        case e => fail(s"Unexpected response: $e")
      }
    }

    "turn async failure into failure response" in {
      val service = create(new AbstractHandler {

        override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[Any] =
          createAsyncReplyEffect(Future.failed(new RuntimeException("boom")))
      })

      val reply =
        LoggingTestKit.error("Failure during handling of command").expect {
          Await.result(service.handleUnary(ActionCommand(serviceName, "Unary", createInPayload("in"))), 10.seconds)
        }
      inside(reply.response) { case ActionResponse.Response.Failure(fail) =>
        fail.description should startWith("Unexpected error")
      }
    }

  }

  private def createOutAny(field: String): Any =
    ActionspecApi.Out.newBuilder().setField(field).build()

  private def createReplyEffect(field: String): Action.Effect[Any] =
    ActionEffectImpl.ReplyEffect(createOutAny(field), None, Nil)

  private def createIgnoreEffect(): Action.Effect[Any] =
    ActionEffectImpl.IgnoreEffect()

  private def createAsyncReplyEffect(future: Future[Action.Effect[Any]]): Action.Effect[Any] =
    ActionEffectImpl.AsyncEffect(future, Nil)

  private def extractInField(message: MessageEnvelope[Any]) =
    message.payload().asInstanceOf[ActionspecApi.In].getField

  private def createInPayload(field: String) =
    Some(ScalaPbAny.fromJavaProto(protobuf.Any.pack(ActionspecApi.In.newBuilder().setField(field).build())))

  private def extractOutField(payload: Option[ScalaPbAny]) =
    ScalaPbAny.toJavaProto(payload.value).unpack(classOf[ActionspecApi.Out]).getField

  class TestAction extends Action

  private abstract class AbstractHandler extends ActionRouter[TestAction](new TestAction) {
    override def handleUnary(commandName: String, message: MessageEnvelope[Any]): Action.Effect[Any] =
      ???

    def handleStreamedOut(commandName: String, message: MessageEnvelope[Any]): Source[Action.Effect[_], NotUsed] = ???

    override def handleStreamedIn(
        commandName: String,
        stream: Source[MessageEnvelope[Any], NotUsed]): Action.Effect[Any] =
      ???

    def handleStreamed(
        commandName: String,
        stream: Source[MessageEnvelope[Any], NotUsed]): Source[Action.Effect[_], NotUsed] = ???
  }

}
