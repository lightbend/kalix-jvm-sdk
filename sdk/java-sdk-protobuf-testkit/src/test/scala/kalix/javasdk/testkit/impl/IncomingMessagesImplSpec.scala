/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit.impl

import scala.collection.mutable

import akka.actor.ActorSystem
import akka.actor.Props
import akka.stream.BoundedSourceQueue
import akka.stream.QueueOfferResult
import akka.stream.scaladsl.Source
import akka.testkit.TestKit
import kalix.eventing.EventSource
import kalix.javasdk.impl.AnySupport
import kalix.javasdk.testkit.impl.EventingTestKitImpl.RunningSourceProbe
import kalix.protocol.component.MetadataEntry
import kalix.protocol.component.MetadataEntry.Value.StringValue
import kalix.testkit.protocol.eventing_test_backend.Message
import kalix.testkit.protocol.eventing_test_backend.SourceElem
import org.scalatest.BeforeAndAfterAll
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class IncomingMessagesImplSpec
    extends TestKit(ActorSystem("MySpec"))
    with AnyWordSpecLike
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private val anySupport = new AnySupport(Array(), getClass.getClassLoader)
  private val subscription =
    new IncomingMessagesImpl(system.actorOf(Props[SourcesHolder](), "holder"), anySupport)
  val queue = new DummyQueue(mutable.Queue.empty)

  private val runningSourceProbe: RunningSourceProbe =
    RunningSourceProbe("dummy-service", EventSource.defaultInstance)(queue, Source.empty[SourceElem])
  subscription.addSourceProbe(runningSourceProbe)

  private val textPlainHeader = MetadataEntry("Content-Type", StringValue("text/plain; charset=utf-8"))
  private val jsonHeader = MetadataEntry("Content-Type", StringValue("application/json"))

  "SubscriptionImpl" must {

    "publish messages from String" in {

      val msg = "hello from test"
      subscription.publish(msg, "test")

      queue.elems.size shouldBe 1
      val SourceElem(Some(Message(payload, md, _)), _, _) = queue.elems.dequeue()
      payload.toStringUtf8 shouldBe msg
      md.get.entries.contains(textPlainHeader) shouldBe true
    }

    "publish messages from jsonable type" in {
      case class DummyMsg(id: Int, test: String)
      val msg = DummyMsg(1, "cool message")
      subscription.publish(msg, msg.id.toString)

      queue.elems.size shouldBe 1
      val SourceElem(Some(Message(payload, md, _)), _, _) = queue.elems.dequeue()
      payload.toStringUtf8 shouldBe """{"id":1,"test":"cool message"}"""
      md.get.entries.contains(jsonHeader) shouldBe true
      assertMetadata(md.get.entries, "ce-subject", msg.id.toString)
    }
  }

  private def assertMetadata(entries: Seq[MetadataEntry], key: String, value: String): Unit = {
    entries.find(_.key == key).get.value.stringValue.get shouldBe value
  }

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}

class DummyQueue(val elems: mutable.Queue[SourceElem]) extends BoundedSourceQueue[SourceElem] {
  override def offer(elem: SourceElem): QueueOfferResult = {
    elems.append(elem)
    QueueOfferResult.Enqueued
  }

  override def complete(): Unit = ???

  override def fail(ex: Throwable): Unit = ???

  override def size(): Int = elems.size
}
