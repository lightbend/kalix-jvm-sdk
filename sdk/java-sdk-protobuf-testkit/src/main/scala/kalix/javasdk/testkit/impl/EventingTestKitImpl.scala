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

package kalix.javasdk.testkit.impl

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.Source
import akka.testkit.TestProbe
import akka.util.BoxedType
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.impl.MessageCodec
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.testkit.EventingTestKit
import kalix.javasdk.testkit.EventingTestKit.Topic
import kalix.javasdk.testkit.impl.TopicImpl.DefaultTimeout
import kalix.testkit.protocol.eventing_test_backend.EmitSingleResult

import java.time
import java.util.{ List => JList }
import kalix.eventing.EventDestination
import kalix.javasdk.{ Metadata => SdkMetadata }
import kalix.javasdk.testkit.EventingTestKit.{ Message => TestKitMessage }
import kalix.protocol.component.Metadata
import com.google.protobuf.ByteString
import kalix.testkit.protocol.eventing_test_backend.EmitSingleCommand
import kalix.testkit.protocol.eventing_test_backend.EventStreamOutCommand
import kalix.testkit.protocol.eventing_test_backend.EventStreamOutResult
import kalix.testkit.protocol.eventing_test_backend.EventingTestKitService
import kalix.testkit.protocol.eventing_test_backend.EventingTestKitServiceHandler
import kalix.testkit.protocol.eventing_test_backend.RunSourceCommand
import kalix.testkit.protocol.eventing_test_backend.SourceElem
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap
import scala.compat.java8.DurationConverters.DurationOps
import scala.compat.java8.OptionConverters.RichOptionalGeneric
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.language.postfixOps
import scala.reflect.ClassTag.Nothing

object EventingTestKitImpl {

  /**
   * Start a pretend eventing backend, configure the proxy to use this through the 'kalix.proxy.eventing.support'
   * "grpc-backend" and the same host and port as this was started with.
   *
   * The returned testkit can be used to expect and emit events to the proxy as if they came from an actual pub/sub
   * event backend.
   */
  def start(system: ActorSystem, host: String, port: Int, decoder: MessageCodec): EventingTestKit = {

    // Create service handlers
    val service = new EventingTestServiceImpl(system, host, port, decoder)
    val handler: HttpRequest => Future[HttpResponse] =
      EventingTestKitServiceHandler(new service.ServiceImpl)(system)

    val binding = Await.result(
      Http(system)
        .newServerAt(host, port)
        .bind(handler),
      10.seconds)
    // to allow binding to 0
    val actualPort = binding.localAddress.getPort
    system.log.info("Eventing testkit grpc-backend started at {}:{}", host, actualPort)
    service.port = actualPort
    service
  }

}

/**
 * Implements the EventingTestKit protocol defined in protocols/testkit/src/main/protobuf/eventing_test_backend.proto
 */
final class EventingTestServiceImpl(system: ActorSystem, val host: String, var port: Int, codec: MessageCodec)
    extends EventingTestKit {

  private val log = LoggerFactory.getLogger(classOf[EventingTestServiceImpl])
  private implicit val sys = system

  private val eventDestinationProbe = new ConcurrentHashMap[EventDestination, TestProbe]()

  override def getTopic(topic: String): Topic = {
    val dest = EventDestination.defaultInstance.withTopic(topic)
    val probe = eventDestinationProbe.computeIfAbsent(dest, _ => TestProbe())
    new TopicImpl(probe, codec)
  }

  final class ServiceImpl extends EventingTestKitService {
    override def emitSingle(in: EmitSingleCommand): Future[EmitSingleResult] = {
      log.info("emitSingle request: [{}]", in)

      in.destination.foreach(dest => {
        val probe = eventDestinationProbe.computeIfAbsent(dest, _ => TestProbe())
        probe.ref ! in
      })

      if (in.destination.isEmpty) {
        log.warn("Received a message without destination, ignoring. {}", in)
      }
      Future.successful(EmitSingleResult())
    }

    override def runSource(in: Source[RunSourceCommand, NotUsed]): Source[SourceElem, NotUsed] = ???

    override def eventStreamOut(in: Source[EventStreamOutCommand, NotUsed]): Source[EventStreamOutResult, NotUsed] =
      throw new UnsupportedOperationException("Feature not supported in the testkit yet")
  }
}

class TopicImpl(private val probe: TestProbe, codec: MessageCodec) extends Topic {

  private val log = LoggerFactory.getLogger(classOf[TopicImpl])

  override def expectNone(): Unit = expectNone(DefaultTimeout)

  override def expectNone(timeout: time.Duration): Unit = probe.expectNoMessage(timeout.toScala)

  override def expectOneRaw(): TestKitMessage[ByteString] = expectOneRaw(DefaultTimeout)

  override def expectOneRaw(timeout: time.Duration): TestKitMessage[ByteString] = {
    val msg = probe.expectMsgType[EmitSingleCommand](timeout.toScala)
    TestKitMessageImpl.ofMessage(msg.getMessage)
  }

  override def expectOne(): TestKitMessage[_] = expectOne(DefaultTimeout)

  override def expectOne(timeout: time.Duration): TestKitMessage[_] = {
    val msg = probe.expectMsgType[EmitSingleCommand](timeout.toScala)
    anyFromMessage(msg.getMessage)
  }

  override def expectOneTyped[T <: GeneratedMessageV3](clazz: Class[T]): TestKitMessage[T] =
    expectOneTyped(clazz, DefaultTimeout)

  override def expectOneTyped[T <: GeneratedMessageV3](clazz: Class[T], timeout: time.Duration): TestKitMessage[T] = {
    val msg = probe.expectMsgType[EmitSingleCommand]
    val metadata = new MetadataImpl(msg.getMessage.getMetadata.entries)
    val decodedMsg = codec.decodeMessage(ScalaPbAny(typeUrlFor(metadata), msg.getMessage.payload))

    val concreteType = TestKitMessageImpl.expectType(decodedMsg, clazz)
    TestKitMessageImpl(concreteType, metadata)
  }

  private def anyFromMessage(m: kalix.testkit.protocol.eventing_test_backend.Message): TestKitMessage[_] = {
    val metadata = new MetadataImpl(m.metadata.getOrElse(Metadata.defaultInstance).entries)
    val anyMsg = codec.decodeMessage(ScalaPbAny(typeUrlFor(metadata), m.payload))
    TestKitMessageImpl(anyMsg, metadata).asInstanceOf[TestKitMessage[_]]
  }

  private def typeUrlFor(metadata: MetadataImpl): String = {
    metadata
      .get("ce-type")
      .orElseGet { () =>
        val contentType = metadata.get("Content-Type").asScala
        contentType match {
          case Some("text/plain; charset=utf-8") => "type.kalix.io/string"
          case Some("application/octet-stream")  => "type.kalix.io/bytes"
          case unknown =>
            log.warn(s"Could not extract typeUrl from $unknown")
            ""
        }
      }
  }

  override def expectN(): JList[TestKitMessage[_]] =
    expectN(Int.MaxValue, DefaultTimeout)

  override def expectN(total: Int): JList[TestKitMessage[_]] =
    expectN(total, DefaultTimeout)

  override def expectN(total: Int, timeout: time.Duration): JList[TestKitMessage[_]] = {
    probe
      .receiveWhile(max = timeout.toScala, messages = total) { case cmd: EmitSingleCommand =>
        anyFromMessage(cmd.getMessage)
      }
      .asJava
  }

  override def clear(): JList[TestKitMessage[_]] = {
    probe
      .receiveWhile(idle = 1.millisecond) { case cmd: EmitSingleCommand =>
        anyFromMessage(cmd.getMessage)
      }
      .asJava
  }

}

object TopicImpl {
  val DefaultTimeout = time.Duration.ofSeconds(3)
}

case class TestKitMessageImpl[P](payload: P, metadata: SdkMetadata) extends TestKitMessage[P] {
  override def getPayload: P = payload
  override def getMetadata: SdkMetadata = metadata

  override def expectType[T <: GeneratedMessageV3](clazz: Class[T]): T = TestKitMessageImpl.expectType(payload, clazz)
}

object TestKitMessageImpl {
  def ofMessage(m: kalix.testkit.protocol.eventing_test_backend.Message): TestKitMessage[ByteString] = {
    val metadata = new MetadataImpl(m.metadata.getOrElse(Metadata.defaultInstance).entries)
    TestKitMessageImpl[ByteString](m.payload, metadata).asInstanceOf[TestKitMessage[ByteString]]
  }

  def expectType[T <: GeneratedMessageV3](payload: Any, clazz: Class[T]): T = {
    val bt = BoxedType(clazz)
    payload match {
      case m if bt.isInstance(m) => m.asInstanceOf[T]
      case m                     => throw new AssertionError(s"Expected $clazz, found ${m.getClass} ($m)")
    }
  }
}
