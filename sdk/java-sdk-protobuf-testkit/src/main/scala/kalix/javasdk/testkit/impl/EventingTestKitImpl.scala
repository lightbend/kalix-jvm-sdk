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
import akka.stream.BoundedSourceQueue
import akka.stream.QueueOfferResult
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.testkit.TestProbe
import akka.util.BoxedType
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.eventing.EventDestination
import kalix.eventing.EventDestination.Destination
import kalix.eventing.EventSource
import kalix.javasdk.Metadata.{ MetadataEntry => SdkMetadataEntry }
import kalix.javasdk.impl.MessageCodec
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.testkit.EventingTestKit
import kalix.javasdk.testkit.EventingTestKit.Topic
import kalix.javasdk.testkit.EventingTestKit.{ Message => TestKitMessage }
import kalix.javasdk.testkit.impl.EventingTestKitImpl.RunningSourceProbe
import kalix.javasdk.testkit.impl.TestKitMessageImpl.defaultMetadata
import kalix.javasdk.testkit.impl.TopicImpl.DefaultTimeout
import kalix.javasdk.{ Metadata => SdkMetadata }
import kalix.protocol.component.Metadata
import kalix.protocol.component.MetadataEntry
import kalix.testkit.protocol.eventing_test_backend.EmitSingleCommand
import kalix.testkit.protocol.eventing_test_backend.EmitSingleResult
import kalix.testkit.protocol.eventing_test_backend.EventStreamOutCommand
import kalix.testkit.protocol.eventing_test_backend.EventStreamOutResult
import kalix.testkit.protocol.eventing_test_backend.EventingTestKitService
import kalix.testkit.protocol.eventing_test_backend.EventingTestKitServiceHandler
import kalix.testkit.protocol.eventing_test_backend.Message
import kalix.testkit.protocol.eventing_test_backend.RunSourceCommand
import kalix.testkit.protocol.eventing_test_backend.RunSourceCreate
import kalix.testkit.protocol.eventing_test_backend.SourceElem
import org.slf4j.LoggerFactory

import java.time
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.{ List => JList }
import scala.compat.java8.DurationConverters.DurationOps
import scala.compat.java8.OptionConverters.RichOptionalGeneric
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.CollectionConverters.IteratorHasAsScala
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.util.Failure
import scala.util.Success
import scala.language.postfixOps

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

  final case class RunningSourceProbe(serviceName: String, source: EventSource)(
      outQueue: BoundedSourceQueue[SourceElem],
      val outSource: Source[SourceElem, NotUsed]) {
    private val log = LoggerFactory.getLogger(classOf[RunningSourceProbe])

    private def emitElement(element: SourceElem): Unit = {
      log.debug("Emitting message {}", element)
      outQueue.offer(element) match {
        case QueueOfferResult.Enqueued    => // goodie
        case QueueOfferResult.Failure(ex) => throw ex
        case QueueOfferResult.Dropped     => throw new AssertionError("Element was dropped")
        case QueueOfferResult.QueueClosed => throw new AssertionError("Queue was closed")
      }
    }

    def emit(data: ByteString, metadata: SdkMetadata): Unit = {

      // FIXME maybe we could improve validation for metadata?
      def convertMetadataEntry(sdkMetadataEntry: SdkMetadataEntry): MetadataEntry = {
        val mde = MetadataEntry(sdkMetadataEntry.getKey)
        if (sdkMetadataEntry.isText) {
          mde.withStringValue(sdkMetadataEntry.getValue)
        } else {
          mde.withBytesValue(ByteString.copyFrom(sdkMetadataEntry.getBinaryValue))
        }
      }

      val testKitMetadata =
        Metadata(metadata.iterator().asScala.map(convertMetadataEntry).toList)

      val subject = metadata.get("ce-subject").orElse("")
      log.debug(
        "Emitting from testkit to test broker, message with metadata={} with subject={}",
        testKitMetadata,
        subject)

      emitElement(SourceElem(Some(Message(data, Some(testKitMetadata))), subject))
    }

  }
}

/**
 * Implements the EventingTestKit protocol originally defined in proxy
 * protocols/testkit/src/main/protobuf/eventing_test_backend.proto
 */
final class EventingTestServiceImpl(system: ActorSystem, val host: String, var port: Int, codec: MessageCodec)
    extends EventingTestKit {

  private val log = LoggerFactory.getLogger(classOf[EventingTestServiceImpl])
  private implicit val sys = system
  private implicit val ec = sys.dispatcher

  private val topics = new ConcurrentHashMap[String, TopicImpl]()

  override def getTopic(topic: String): Topic = getTopicImpl(topic)

  private def getTopicImpl(topic: String): TopicImpl =
    topics.computeIfAbsent(topic, _ => new TopicImpl(TestProbe(), TestProbe(), codec))

  final class ServiceImpl extends EventingTestKitService {
    override def emitSingle(in: EmitSingleCommand): Future[EmitSingleResult] = {
      log.debug("Receiving message from test broker: [{}]", in)

      in.destination.foreach(dest => {
        val probe = getTopicImpl(dest.getTopic).destinationProbe
        probe.ref ! in
      })

      if (in.destination.isEmpty) {
        log.warn("Received a message without destination, ignoring. {}", in)
      }
      Future.successful(EmitSingleResult())
    }

    override def runSource(in: Source[RunSourceCommand, NotUsed]): Source[SourceElem, NotUsed] = {
      log.debug("Reading topic from test broker - runSource request started: {}", in)
      val runningSourcePromise = Promise[RunningSourceProbe]()

      in.watchTermination() { (_, fDone) =>
        fDone.onComplete {
          case Success(_)  => log.debug("runSource in completed")
          case Failure(ex) => log.error("runSource in failed", ex)
        }
      }.runWith(Sink.fold(None: Option[RunningSourceProbe]) {
        case (
              None,
              RunSourceCommand(
                RunSourceCommand.Command.Create(RunSourceCreate(serviceName, Some(eventSource), _)),
                _)) =>
          // proxy triggers probe creation through the create command
          log.debug(
            "runSource request got initial create command for service name [{}], source: [{}]",
            serviceName,
            eventSource)
          val (queue, source) = Source.queue[SourceElem](10).preMaterialize()
          val runningSourceProbe = RunningSourceProbe(serviceName, eventSource)(queue, source)
          getTopicImpl(eventSource.getTopic).sourceProbe.ref ! runningSourceProbe
          runningSourcePromise.success(runningSourceProbe)
          Some(runningSourceProbe)

        case (s @ Some(_), RunSourceCommand(RunSourceCommand.Command.Ack(sourceAck), _)) =>
          log.debug("runSource request got ack [{}]", sourceAck)
          s
        case wat => throw new MatchError(s"Unexpected fold input: $wat")
      })

      Source
        .futureSource(runningSourcePromise.future.map { runningSourceProbe =>
          runningSourceProbe.outSource
        })
        .watchTermination() { (_, fDone) =>
          fDone.onComplete {
            case Success(_)  => log.debug("runSource out completed")
            case Failure(ex) => log.error("runSource out failed", ex)
          }
        }
        .mapMaterializedValue(_ => NotUsed)
    }

    override def eventStreamOut(in: Source[EventStreamOutCommand, NotUsed]): Source[EventStreamOutResult, NotUsed] =
      throw new UnsupportedOperationException("Feature not supported in the testkit yet")
  }
}

private[testkit] class TopicImpl(
    private[testkit] val destinationProbe: TestProbe,
    private[testkit] val sourceProbe: TestProbe,
    codec: MessageCodec)
    extends Topic {

  private lazy val brokerProbe = sourceProbe.expectMsgType[RunningSourceProbe]

  private val log = LoggerFactory.getLogger(classOf[TopicImpl])

  override def expectNone(): Unit = expectNone(DefaultTimeout)

  override def expectNone(timeout: time.Duration): Unit = destinationProbe.expectNoMessage(timeout.toScala)

  override def expectOneRaw(): TestKitMessage[ByteString] = expectOneRaw(DefaultTimeout)

  override def expectOneRaw(timeout: time.Duration): TestKitMessage[ByteString] = {
    val msg = destinationProbe.expectMsgType[EmitSingleCommand](timeout.toScala)
    TestKitMessageImpl.ofProtocolMessage(msg.getMessage)
  }

  override def expectOne(): TestKitMessage[_] = expectOne(DefaultTimeout)

  override def expectOne(timeout: time.Duration): TestKitMessage[_] = {
    val msg = destinationProbe.expectMsgType[EmitSingleCommand](timeout.toScala)
    anyFromMessage(msg.getMessage)
  }

  override def expectOneTyped[T <: GeneratedMessageV3](clazz: Class[T]): TestKitMessage[T] =
    expectOneTyped(clazz, DefaultTimeout)

  override def expectOneTyped[T <: GeneratedMessageV3](clazz: Class[T], timeout: time.Duration): TestKitMessage[T] = {
    val msg = destinationProbe.expectMsgType[EmitSingleCommand]
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
    destinationProbe
      .receiveWhile(max = timeout.toScala, messages = total) { case cmd: EmitSingleCommand =>
        anyFromMessage(cmd.getMessage)
      }
      .asJava
  }

  override def clear(): JList[TestKitMessage[_]] = {
    destinationProbe
      .receiveWhile(idle = 50.millisecond) { case cmd: EmitSingleCommand =>
        anyFromMessage(cmd.getMessage)
      }
      .asJava
  }

  override def publish(message: ByteString): Unit =
    publish(message, SdkMetadata.EMPTY)

  override def publish(message: ByteString, metadata: SdkMetadata): Unit =
    brokerProbe.emit(message, metadata)

  override def publish[T <: GeneratedMessageV3](message: TestKitMessage[T]): Unit =
    publish(message.getPayload.toByteString, message.getMetadata)

  override def publish[T <: GeneratedMessageV3](message: T, subject: String): Unit = {
    val md = defaultMetadata(message, subject)
    publish(message.toByteString, md)
  }

  override def publish[T <: GeneratedMessageV3](message: JList[TestKitMessage[T]]): Unit =
    message.asScala.foreach(m => publish(m))

}

private[testkit] object TopicImpl {
  val DefaultTimeout = time.Duration.ofSeconds(3)
}

private[testkit] case class TestKitMessageImpl[P](payload: P, metadata: SdkMetadata) extends TestKitMessage[P] {
  override def getPayload: P = payload
  override def getMetadata: SdkMetadata = metadata

  override def expectType[T <: GeneratedMessageV3](clazz: Class[T]): T = TestKitMessageImpl.expectType(payload, clazz)
}

private[testkit] object TestKitMessageImpl {
  def ofProtocolMessage(m: kalix.testkit.protocol.eventing_test_backend.Message): TestKitMessage[ByteString] = {
    val metadata = new MetadataImpl(m.metadata.getOrElse(Metadata()).entries)
    TestKitMessageImpl[ByteString](m.payload, metadata).asInstanceOf[TestKitMessage[ByteString]]
  }

  def of[T <: GeneratedMessageV3](payload: T): TestKitMessage[T] =
    TestKitMessageImpl(payload, defaultMetadata(payload, ""))

  def defaultMetadata(message: GeneratedMessageV3, subject: String): SdkMetadata =
    SdkMetadata.EMPTY
      .add("ce-specversion", "1.0")
      .add("ce-id", UUID.randomUUID().toString)
      .add("ce-subject", subject)
      .add("Content-Type", "application/protobuf;proto=" + message.getDescriptorForType.getFullName)
      .add("ce-type", message.getDescriptorForType.getName)
      .add("ce-source", message.getDescriptorForType.getFullName)

  def expectType[T <: GeneratedMessageV3](payload: Any, clazz: Class[T]): T = {
    val bt = BoxedType(clazz)
    payload match {
      case m if bt.isInstance(m) => m.asInstanceOf[T]
      case m                     => throw new AssertionError(s"Expected $clazz, found ${m.getClass} ($m)")
    }
  }
}
