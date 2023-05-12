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
import akka.annotation.ApiMayChange
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.stream.scaladsl.Source
import akka.testkit.TestProbe
import com.google.protobuf.GeneratedMessageV3
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.testkit.EventingTestKit
import kalix.javasdk.testkit.EventingTestKit.Topic
import kalix.javasdk.testkit.impl.TopicImpl.DefaultTimeout
import kalix.testkit.protocol.eventing_test_backend.EmitSingleResult

import java.time
import java.util
import java.util.concurrent.ConcurrentHashMap
import scala.compat.java8.DurationConverters.DurationOps
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.language.postfixOps
//import akka.testkit.TestProbe
import kalix.eventing.EventDestination
import kalix.javasdk.testkit.EventingTestKit.{ Message => TestKitMessage }
import kalix.javasdk.{ Metadata => SdkMetadata }
import kalix.protocol.component.Metadata

//import kalix.javasdk.testkit.impl.EventingTestKit.EventStreamOutProbe
//import kalix.javasdk.testkit.impl.EventingTestKit.RunningSourceProbe
//import kalix.protocol.eventing_test_backend._
import com.google.protobuf.ByteString
import kalix.testkit.protocol.eventing_test_backend.EmitSingleCommand
import kalix.testkit.protocol.eventing_test_backend.EventStreamOutCommand
import kalix.testkit.protocol.eventing_test_backend.EventStreamOutResult
import kalix.testkit.protocol.eventing_test_backend.EventingTestKitService
import kalix.testkit.protocol.eventing_test_backend.EventingTestKitServiceHandler
import kalix.testkit.protocol.eventing_test_backend.RunSourceCommand
import kalix.testkit.protocol.eventing_test_backend.SourceElem
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

object EventingTestKitImpl {

  /**
   * Start a pretend eventing backend, configure the proxy to use this through the 'kalix.proxy.eventing.support'
   * "grpc-backend" and the same host and port as this was started with.
   *
   * The returned testkit can be used to expect and emit events to the proxy as if they came from an actual pub/sub
   * event backend.
   */
  def start(system: ActorSystem, host: String, port: Int): EventingTestKit = {

    // Create service handlers
    val service = new EventingTestServiceImpl(system, host, port)
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
private[testkit] final class EventingTestServiceImpl(system: ActorSystem, val host: String, var port: Int)
    extends EventingTestKit {

  private val log = LoggerFactory.getLogger(classOf[EventingTestKit])
  private implicit val sys = system

  private val eventDestinationProbe = new ConcurrentHashMap[EventDestination, TestProbe]()

  override def getTopic(topic: String): Topic = {
    val dest = EventDestination.defaultInstance.withTopic(topic)
    val probe = eventDestinationProbe.computeIfAbsent(dest, _ => TestProbe())
    new TopicImpl(probe)
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

    override def eventStreamOut(in: Source[EventStreamOutCommand, NotUsed]): Source[EventStreamOutResult, NotUsed] = throw new UnsupportedOperationException("Feature not supported in the testkit yet")
  }
}

class TopicImpl(private val probe: TestProbe) extends Topic {
  @ApiMayChange
  override def expectOne(): TestKitMessage[ByteString] = expectOne(DefaultTimeout)

  @ApiMayChange
  override def expectOne(timeout: time.Duration): TestKitMessage[ByteString] = {
    val msg = probe.expectMsgType[EmitSingleCommand](timeout.toScala)
    TestKitMessageImpl.ofMessage(msg.getMessage)
  }

  @ApiMayChange
  override def expectOneClassOf[T <: GeneratedMessageV3](instance: T): TestKitMessage[T] = {
    val msg = expectOne()
    new TestKitMessageImpl(
      instance.getParserForType.parseFrom(msg.getPayload.toByteArray).asInstanceOf[T],
      msg.getMetadata)
  }

  @ApiMayChange
  override def expectN(): util.Collection[TestKitMessage[ByteString]] =
    expectN(Int.MaxValue, DefaultTimeout)

  @ApiMayChange
  override def expectN(total: Int): util.Collection[TestKitMessage[ByteString]] =
    expectN(total, DefaultTimeout)

  @ApiMayChange
  override def expectN(total: Int, timeout: time.Duration): util.Collection[TestKitMessage[ByteString]] = {
    probe
      .receiveWhile(max = timeout.toScala, messages = total) { case m: EmitSingleCommand =>
        TestKitMessageImpl.ofMessage(m.getMessage)
      }
      .asJava
  }

}

object TopicImpl {
  val DefaultTimeout = time.Duration.ofSeconds(3)
}

class TestKitMessageImpl[P](payload: P, metadata: SdkMetadata) extends TestKitMessage[P] {
  def getPayload(): P = payload
  def getMetadata(): SdkMetadata = metadata
}

object TestKitMessageImpl {
  def ofMessage(m: kalix.testkit.protocol.eventing_test_backend.Message): TestKitMessage[ByteString] = {
    val metadata = new MetadataImpl(m.metadata.getOrElse(Metadata.defaultInstance).entries)
    new TestKitMessageImpl[ByteString](m.payload, metadata).asInstanceOf[TestKitMessage[ByteString]]
  }
}
