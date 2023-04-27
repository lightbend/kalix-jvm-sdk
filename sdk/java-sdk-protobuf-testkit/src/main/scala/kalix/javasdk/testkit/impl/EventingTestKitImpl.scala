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
import akka.http.scaladsl.model.headers.CacheDirectives.public
import akka.stream.BoundedSourceQueue
import akka.stream.QueueOfferResult
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import akka.testkit.TestProbe
import kalix.javasdk.impl.MetadataImpl
import kalix.javasdk.testkit.EventingTestKit
import kalix.javasdk.testkit.EventingTestKit.Topic
import kalix.testkit.protocol.eventing_test_backend.EmitSingleResult
import org.checkerframework.checker.units.qual.{ m, s }

import java.util
import java.util.concurrent.{ ConcurrentHashMap, ConcurrentLinkedQueue }
import scala.collection.mutable
import scala.concurrent.java8.FuturesConvertersImpl.P
import scala.jdk.CollectionConverters.SeqHasAsJava
import scala.language.postfixOps
//import akka.testkit.TestProbe
import akka.testkit
import kalix.eventing.EventDestination
import kalix.eventing.EventSource
import kalix.protocol.component.Metadata
import kalix.protocol.component.MetadataEntry
import kalix.javasdk.{ Metadata => SdkMetadata }
import kalix.javasdk.testkit.EventingTestKit.{ Message => TestKitMessage }

//import kalix.javasdk.testkit.impl.EventingTestKit.EventStreamOutProbe
//import kalix.javasdk.testkit.impl.EventingTestKit.RunningSourceProbe
//import kalix.protocol.eventing_test_backend._
import com.google.protobuf.ByteString
import org.slf4j.LoggerFactory
import scalapb.GeneratedMessage
import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.BoundedSourceQueue
import kalix.testkit.protocol.eventing_test_backend.{
  EmitSingleCommand,
  EventStreamOutCommand,
  EventStreamOutElement,
  EventStreamOutResult,
  EventingTestKitService,
  EventingTestKitServiceHandler,
  RunSourceCommand,
  SourceElem
}

import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

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

  override def getHost(): String = host
  override def getPort(): Integer = port

  private val log = LoggerFactory.getLogger(classOf[EventingTestKit])
  private implicit val sys = system
  private implicit val ec = sys.dispatcher

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

    override def eventStreamOut(in: Source[EventStreamOutCommand, NotUsed]): Source[EventStreamOutResult, NotUsed] = ???
  }
}

class TopicImpl(private val probe: TestProbe) extends Topic {
  @ApiMayChange
  override def expectNext(): TestKitMessage[ByteString] = {
    val msg = probe.expectMsgType[EmitSingleCommand]
    TestKitMessageImpl.ofMessage(msg.getMessage)
  }

  @ApiMayChange
  override def expectAll(): util.Collection[TestKitMessage[ByteString]] = {
    probe
      .receiveWhile(3 seconds) { case m: EmitSingleCommand =>
        TestKitMessageImpl.ofMessage(m.getMessage)
      }
      .asJava
  }

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
