/*
 * Copyright 2024 Lightbend Inc.
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

import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.testkit.TestProbe
import com.google.protobuf.ByteString
import kalix.eventing.EventDestination
import kalix.eventing.EventDestination.Destination.Topic
import kalix.javasdk.impl.AnySupport
import kalix.protocol.component.Metadata
import kalix.protocol.component.MetadataEntry
import kalix.protocol.component.MetadataEntry.Value.StringValue
import kalix.testkit.protocol.eventing_test_backend.EmitSingleCommand
import kalix.testkit.protocol.eventing_test_backend.Message
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class OutgoingMessagesImplSpec
    extends TestKit(ActorSystem("MySpec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private val anySupport = new AnySupport(Array(), getClass.getClassLoader)
  private val outProbe = TestProbe()(system)
  private val destination = new OutgoingMessagesImpl(outProbe, anySupport)
  val queue = new DummyQueue(mutable.Queue.empty)

  private val textPlainHeader = MetadataEntry("Content-Type", StringValue("text/plain; charset=utf-8"))
  private val bytesHeader = MetadataEntry("Content-Type", StringValue("application/octet-stream"))
  private def msgWithMetadata(any: Any, mdEntry: MetadataEntry*) = EmitSingleCommand(
    Some(EventDestination(Topic("test-topic"))),
    Some(Message(anySupport.encodeScala(any).value, Some(Metadata(mdEntry)))))

  "TopicImpl" must {
    "provide utility to read typed messages - string" in {
      val msg = "this is a message"
      outProbe.ref ! msgWithMetadata(msg, textPlainHeader)

      val receivedMsg = destination.expectN(1).get(0).expectType(classOf[com.google.protobuf.StringValue])
      receivedMsg.getValue shouldBe msg
    }

    "provide utility to read typed messages - bytes" in {
      val bytes = ByteString.copyFromUtf8("this is a message")
      outProbe.ref ! msgWithMetadata(bytes, bytesHeader)

      val receivedMsg = destination.expectOneTyped(classOf[com.google.protobuf.BytesValue])
      receivedMsg.getPayload.getValue shouldBe bytes
    }

    "fail when next msg is not of expected type" in {
      val msg = "this is a message"
      val bytes = ByteString.copyFromUtf8("this is a message")

      outProbe.ref ! msgWithMetadata(msg, textPlainHeader)
      outProbe.ref ! msgWithMetadata(bytes, bytesHeader)

      assertThrows[AssertionError] {
        // we are expecting the second msg type so this fails when it receives the first one
        destination.expectOneTyped(classOf[com.google.protobuf.BytesValue])
      }
    }

    "provide utility to read multiple messages" in {
      val msg = "this is a message"
      val msg2 = "this is a second message"
      val msg3 = "this is a third message, to read later"
      outProbe.ref ! msgWithMetadata(msg, textPlainHeader)
      outProbe.ref ! msgWithMetadata(msg2, textPlainHeader)
      outProbe.ref ! msgWithMetadata(msg3, textPlainHeader)

      val Seq(received1, received2) = destination.expectN(2).asScala.toSeq
      received1.expectType(classOf[com.google.protobuf.StringValue]).getValue shouldBe msg
      received2.expectType(classOf[com.google.protobuf.StringValue]).getValue shouldBe msg2

      // third message was there already but was not read yet
      destination.expectOne().expectType(classOf[com.google.protobuf.StringValue]).getValue shouldBe msg3
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    // for tests when we are expecting a failure, some messages might remain unread and mess up with following tests, thus clearing
    destination.clear()
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
