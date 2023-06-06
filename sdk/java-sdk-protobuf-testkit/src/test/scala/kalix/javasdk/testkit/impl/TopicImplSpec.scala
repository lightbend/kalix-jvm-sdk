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

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.jdk.CollectionConverters.CollectionHasAsScala

class TopicImplSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike with Matchers with BeforeAndAfterEach {

  private val anySupport = new AnySupport(Array(), getClass.getClassLoader)
  private val outProbe = TestProbe()(system.classicSystem)
  private val inProbe = TestProbe()(system.classicSystem)
  private val topic = new TopicImpl(outProbe, inProbe, anySupport)

  private val textPlainMd = MetadataEntry("Content-Type", StringValue("text/plain; charset=utf-8"))
  private val bytesMd = MetadataEntry("Content-Type", StringValue("application/octet-stream"))
  private def msgWithMetadata(any: Any, mdEntry: MetadataEntry*) = EmitSingleCommand(
    Some(EventDestination(Topic("test-topic"))),
    Some(Message(anySupport.encodeScala(any).value, Some(Metadata(mdEntry)))))

  "TopicImpl" must {
    "provide utility to read typed messages - string" in {
      val msg = "this is a message"
      outProbe.ref ! msgWithMetadata(msg, textPlainMd)

      val receivedMsg = topic.expectN(1).get(0).expectType(classOf[com.google.protobuf.StringValue])
      receivedMsg.getValue shouldBe msg
    }

    "provide utility to read typed messages - bytes" in {
      val bytes = ByteString.copyFromUtf8("this is a message")
      outProbe.ref ! msgWithMetadata(bytes, bytesMd)

      val receivedMsg = topic.expectOneTyped(classOf[com.google.protobuf.BytesValue])
      receivedMsg.getPayload.getValue shouldBe bytes
    }

    "fail when next msg is not of expected type" in {
      val msg = "this is a message"
      val bytes = ByteString.copyFromUtf8("this is a message")

      outProbe.ref ! msgWithMetadata(msg, textPlainMd)
      outProbe.ref ! msgWithMetadata(bytes, bytesMd)

      assertThrows[AssertionError] {
        // we are expecting the second msg type so this fails when it receives the first one
        topic.expectOneTyped(classOf[com.google.protobuf.BytesValue])
      }
    }

    "provide utility to read multiple messages" in {
      val msg = "this is a message"
      val msg2 = "this is a second message"
      val msg3 = "this is a third message, to read later"
      outProbe.ref ! msgWithMetadata(msg, textPlainMd)
      outProbe.ref ! msgWithMetadata(msg2, textPlainMd)
      outProbe.ref ! msgWithMetadata(msg3, textPlainMd)

      val Seq(received1, received2) = topic.expectN(2).asScala.toSeq
      received1.expectType(classOf[com.google.protobuf.StringValue]).getValue shouldBe msg
      received2.expectType(classOf[com.google.protobuf.StringValue]).getValue shouldBe msg2

      // third message was there already but was not read yet
      topic.expectOne().expectType(classOf[com.google.protobuf.StringValue]).getValue shouldBe msg3
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    // for tests when we are expecting a failure, some messages might remain unread and mess up with following tests, thus clearing
    topic.clear()
  }
}
