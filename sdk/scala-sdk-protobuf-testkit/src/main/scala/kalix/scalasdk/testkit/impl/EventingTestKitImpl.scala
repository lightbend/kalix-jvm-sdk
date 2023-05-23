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

import akka.util.BoxedType
import com.google.protobuf.ByteString
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.impl.MessageCodec
import kalix.javasdk.testkit.{ EventingTestKit => JEventingTestKit }
import kalix.scalasdk.impl.MetadataConverters
import kalix.scalasdk.testkit.Message
import kalix.scalasdk.testkit.Topic
import scalapb.GeneratedMessage

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.DurationConverters.ScalaDurationOps
import scala.language.postfixOps
import scala.reflect.ClassTag

case class TopicImpl private (delegate: JEventingTestKit.Topic, codec: MessageCodec) extends Topic {

  override def expectNone(): Unit = delegate.expectNone()

  override def expectNone(timeout: FiniteDuration): Unit = delegate.expectNone(timeout.toJava)

  override def expectOneRaw(): Message[ByteString] = {
    val msg = delegate.expectOneRaw()
    Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata))
  }

  override def expectOneRaw(timeout: FiniteDuration): Message[ByteString] = {
    val msg = delegate.expectOneRaw(timeout.toJava)
    Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata))
  }

  override def expectOne(): Message[_] = {
    val msg = delegate.expectOne()
    Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata))
  }

  override def expectOne(timeout: FiniteDuration): Message[_] = {
    val msg = delegate.expectOne(timeout.toJava)
    Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata))
  }

  override def expectMessageType[T <: GeneratedMessage](implicit t: ClassTag[T]): Message[T] = {
    val msg = delegate.expectOneRaw()
    expectMessageType_internal(msg)
  }

  override def expectMessageType[T <: GeneratedMessage](timeout: FiniteDuration)(implicit
      t: ClassTag[T]): Message[T] = {
    val msg = delegate.expectOneRaw(timeout.toJava)
    expectMessageType_internal(msg)
  }

  private def expectMessageType_internal[T <: GeneratedMessage](msg: JEventingTestKit.Message[ByteString])(implicit
      t: ClassTag[T]): Message[T] = {
    val payloadType = msg.getMetadata.get("ce-type").orElse("")
    val decodedMsg = codec.decodeMessage(ScalaPbAny(payloadType, msg.getPayload))

    val concreteType = MessageImpl.expectType(decodedMsg)
    Message(concreteType, MetadataConverters.toScala(msg.getMetadata))
  }
  override def expectN(): Seq[Message[_]] = expectN(Int.MaxValue)

  override def expectN(total: Int): Seq[Message[_]] = {
    val allMsg = delegate.expectN(total)
    allMsg.asScala
      .map(msg => Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata)))
      .toSeq
  }

  override def expectN(total: Int, timeout: FiniteDuration): Seq[Message[_]] = {
    val allMsg = delegate.expectN(total, timeout.toJava)
    allMsg.asScala
      .map(msg => Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata)))
      .toSeq
  }
}

object MessageImpl {
  def expectType[T <: GeneratedMessage](payload: Any)(implicit t: ClassTag[T]): T = {
    val bt = BoxedType(t.runtimeClass)
    payload match {
      case m if bt.isInstance(m) => m.asInstanceOf[T]
      case m                     => throw new AssertionError(s"Expected $t, found ${m.getClass} ($m)")
    }
  }
}
