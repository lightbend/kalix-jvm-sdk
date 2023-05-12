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

package kalix.scalasdk.testkit.impl

import com.google.protobuf.ByteString
import kalix.javasdk.testkit.{ EventingTestKit => JEventingTestKit }
import kalix.scalasdk.impl.MetadataConverters
import kalix.scalasdk.testkit.Message
import kalix.scalasdk.testkit.Topic
import scalapb.GeneratedMessage
import scalapb.GeneratedMessageCompanion

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.DurationConverters.ScalaDurationOps
import scala.language.postfixOps

case class TopicImpl private (delegate: JEventingTestKit.Topic) extends Topic {
  override def expectOne(): Message[ByteString] = {
    val msg = delegate.expectOne()
    Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata))
  }

  override def expectOne(timeout: FiniteDuration): Message[ByteString] = {
    val msg = delegate.expectOne(timeout.toJava)
    Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata))
  }

  override def expectOneClassOf[T <: GeneratedMessage](companion: GeneratedMessageCompanion[T]): Message[T] = {
    val msg = expectOne()
    Message(companion.parseFrom(msg.payload.toByteArray), msg.metadata)
  }

  override def expectN(): Seq[Message[ByteString]] = expectN(Int.MaxValue)

  override def expectN(total: Int): Seq[Message[ByteString]] = {
    val allMsg = delegate.expectN(total)
    allMsg.asScala
      .map(msg => Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata)))
      .toSeq
  }

  override def expectN(total: Int, timeout: FiniteDuration): Seq[Message[ByteString]] = {
    val allMsg = delegate.expectN(total, timeout.toJava)
    allMsg.asScala
      .map(msg => Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata)))
      .toSeq
  }
}
