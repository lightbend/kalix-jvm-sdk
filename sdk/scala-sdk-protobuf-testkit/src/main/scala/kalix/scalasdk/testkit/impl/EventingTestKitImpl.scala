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

package kalix.scalasdk.testkit.impl

import akka.util.BoxedType
import com.google.protobuf.ByteString
import com.google.protobuf.any.{ Any => ScalaPbAny }
import kalix.javasdk.impl.MessageCodec
import kalix.javasdk.testkit.{ EventingTestKit => JEventingTestKit }
import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.MetadataConverters
import kalix.scalasdk.testkit.Message
import kalix.scalasdk.testkit.Topic
import kalix.scalasdk.testkit.impl.MessageImpl.defaultMetadata
import org.slf4j.LoggerFactory
import scalapb.GeneratedMessage
import java.util.UUID
import java.util.{ List => JList }

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.jdk.DurationConverters.ScalaDurationOps
import scala.language.postfixOps
import scala.reflect.ClassTag

import kalix.scalasdk.testkit.IncomingMessages
import kalix.scalasdk.testkit.OutgoingMessages

private[testkit] case class IncomingMessagesImpl private (delegate: JEventingTestKit.IncomingMessages)
    extends IncomingMessages {

  override def publish(message: ByteString): Unit = delegate.publish(message)

  override def publish(message: ByteString, metadata: Metadata): Unit =
    delegate.publish(message, MetadataConverters.toJava(metadata))

  override def publish[T <: GeneratedMessage](message: Message[T]): Unit =
    publish(message.payload.toByteString, message.metadata)

  override def publish[T <: GeneratedMessage](message: T, subject: String): Unit =
    publish(message.toByteString, defaultMetadata(message, subject))

  override def publish[T <: GeneratedMessage](messages: List[Message[T]]): Unit =
    messages.foreach(m => publish(m))

  override def publishDelete(subject: String): Unit = delegate.publishDelete(subject)
}

private[testkit] case class OutgoingMessagesImpl private (
    delegate: JEventingTestKit.OutgoingMessages,
    codec: MessageCodec)
    extends OutgoingMessages {

  private val log = LoggerFactory.getLogger(classOf[TopicImpl])

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

  override def expectOneTyped[T <: GeneratedMessage](implicit t: ClassTag[T]): Message[T] = {
    val msg = delegate.expectOneTyped(t.runtimeClass.asInstanceOf[Class[T]])
    val md = MetadataConverters.toScala(msg.getMetadata)
    Message(msg.getPayload, md)
  }

  override def expectOneTyped[T <: GeneratedMessage](timeout: FiniteDuration)(implicit t: ClassTag[T]): Message[T] = {
    val msg = delegate.expectOneTyped(t.runtimeClass.asInstanceOf[Class[T]], timeout.toJava)
    val md = MetadataConverters.toScala(msg.getMetadata)
    Message(msg.getPayload, md)
  }

  override def expectN(): Seq[Message[_]] = expectN(Int.MaxValue)

  override def expectN(total: Int): Seq[Message[_]] = {
    val allMsgs = delegate.expectN(total)
    msgsAsJava(allMsgs)
  }

  override def expectN(total: Int, timeout: FiniteDuration): Seq[Message[_]] = {
    val allMsgs = delegate.expectN(total, timeout.toJava)
    msgsAsJava(allMsgs)
  }

  override def clear(): Seq[Message[_]] = {
    val clearedMsgs = delegate.clear()
    msgsAsJava(clearedMsgs)
  }

  private def msgsAsJava(msgs: JList[JEventingTestKit.Message[_]]) = {
    msgs.asScala
      .map(msg => Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata)))
      .toSeq
  }
}

private[testkit] case class TopicImpl private (delegate: JEventingTestKit.Topic, codec: MessageCodec) extends Topic {

  private val log = LoggerFactory.getLogger(classOf[TopicImpl])

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

  override def expectOneTyped[T <: GeneratedMessage](implicit t: ClassTag[T]): Message[T] = {
    val msg = delegate.expectOneTyped(t.runtimeClass.asInstanceOf[Class[T]])
    val md = MetadataConverters.toScala(msg.getMetadata)
    Message(msg.getPayload, md)
  }

  override def expectOneTyped[T <: GeneratedMessage](timeout: FiniteDuration)(implicit t: ClassTag[T]): Message[T] = {
    val msg = delegate.expectOneTyped(t.runtimeClass.asInstanceOf[Class[T]], timeout.toJava)
    val md = MetadataConverters.toScala(msg.getMetadata)
    Message(msg.getPayload, md)
  }

  override def expectN(): Seq[Message[_]] = expectN(Int.MaxValue)

  override def expectN(total: Int): Seq[Message[_]] = {
    val allMsgs = delegate.expectN(total)
    msgsAsJava(allMsgs)
  }

  override def expectN(total: Int, timeout: FiniteDuration): Seq[Message[_]] = {
    val allMsgs = delegate.expectN(total, timeout.toJava)
    msgsAsJava(allMsgs)
  }

  override def clear(): Seq[Message[_]] = {
    val clearedMsgs = delegate.clear()
    msgsAsJava(clearedMsgs)
  }

  private def msgsAsJava(msgs: JList[JEventingTestKit.Message[_]]) = {
    msgs.asScala
      .map(msg => Message(msg.getPayload, MetadataConverters.toScala(msg.getMetadata)))
      .toSeq
  }

  override def publish(message: ByteString): Unit = delegate.publish(message)

  override def publish(message: ByteString, metadata: Metadata): Unit =
    delegate.publish(message, MetadataConverters.toJava(metadata))

  override def publish[T <: GeneratedMessage](message: Message[T]): Unit =
    publish(message.payload.toByteString, message.metadata)

  override def publish[T <: GeneratedMessage](message: T, subject: String): Unit =
    publish(message.toByteString, defaultMetadata(message, subject))

  override def publish[T <: GeneratedMessage](messages: List[Message[T]]): Unit =
    messages.foreach(m => publish(m))
}

private[testkit] object MessageImpl {
  def expectType[T <: GeneratedMessage](payload: Any)(implicit t: ClassTag[T]): T = {
    val bt = BoxedType(t.runtimeClass)
    payload match {
      case m if bt.isInstance(m) => m.asInstanceOf[T]
      case m                     => throw new AssertionError(s"Expected $t, found ${m.getClass} ($m)")
    }
  }

  def defaultMetadata(message: GeneratedMessage, subject: String): Metadata =
    Metadata.empty
      .add("ce-specversion", "1.0")
      .add("ce-id", UUID.randomUUID().toString)
      .add("ce-subject", subject)
      .add("Content-Type", "application/protobuf;proto=" + message.companion.javaDescriptor.getFullName)
      .add("ce-type", message.companion.javaDescriptor.getName)
      .add("ce-source", message.companion.javaDescriptor.getFullName)
}
