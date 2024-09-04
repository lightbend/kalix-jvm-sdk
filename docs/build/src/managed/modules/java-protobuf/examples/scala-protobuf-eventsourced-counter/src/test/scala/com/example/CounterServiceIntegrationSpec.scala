package com.example

import com.example.actions.{ Decreased, Increased }
import kalix.scalasdk.CloudEvent

import java.net.URI
// tag::test-topic[]
import kalix.scalasdk.testkit.{ KalixTestKit, Message }
import org.scalatest.BeforeAndAfterEach
// ...
// end::test-topic[]
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

import scala.language.postfixOps

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::test-topic[]

class CounterServiceIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures {

  // end::test-topic[]
  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  // tag::test-topic[]
  private val testKit = KalixTestKit(
    Main.createKalix(),
    KalixTestKit.DefaultSettings
      // end::test-topic[]
      .withTopicOutgoingMessages("counter-events-with-meta")
      // tag::test-topic[]
      .withTopicIncomingMessages("counter-commands")
      .withTopicOutgoingMessages("counter-events")).start() // <1>
  // end::test-topic[]

  private val client = testKit.getGrpcClient(classOf[CounterService])

  // tag::test-topic[]
  private val commandsTopic = testKit.getTopicIncomingMessages("counter-commands") // <2>
  private val eventsTopic = testKit.getTopicOutgoingMessages("counter-events") // <3>
  // end::test-topic[]

  private val eventsTopicWithMeta = testKit.getTopicOutgoingMessages("counter-events-with-meta")

  // tag::clear-topics[]
  override def beforeEach(): Unit = { // <1>
    eventsTopic.clear() // <2>
    eventsTopicWithMeta.clear()
  }
  // end::clear-topics[]

  // tag::test-topic[]

  "CounterService" must {
    val counterId = "xyz"
    // end::test-topic[]

    "handle side effect that adds the initial input multiplied by two and verify publishing" in {

      client.increaseWithSideEffect(IncreaseValue(counterId, 10)).futureValue
      val counter = client.getCurrentCounter(GetCounter(counterId)).futureValue
      counter.value shouldBe (10 + 10 * 2)

      // verify messages published to topic
      val allMsgs = eventsTopic.expectN(2)

      val Seq(Message(payload1, md1), Message(payload2, md2)) = allMsgs
      payload1 shouldBe Increased(10)
      md1.get("ce-type") should contain(classOf[Increased].getName)
      md1.get("Content-Type") should contain("application/protobuf")

      payload2 shouldBe Increased(20)
      md2.get("ce-type") should contain(classOf[Increased].getName)
      md2.get("Content-Type") should contain("application/protobuf")
    }

    "handle decrease for the same counter and verify publishing" in {
      client.decrease(DecreaseValue(counterId, 15)).futureValue
      val counter = client.getCurrentCounter(GetCounter(counterId)).futureValue
      counter.value shouldBe 15

      // verify message published to topic
      val Message(decEvent, md) = eventsTopic.expectOneTyped[Decreased]
      decEvent shouldBe Decreased(15)
      md.get("ce-type") should contain(classOf[Decreased].getName)
      md.get("Content-Type") should contain("application/protobuf")
    }

    // tag::test-topic[]
    "handle commands from topic and publishing related events out" in {
      commandsTopic.publish(IncreaseValue(counterId, 4), counterId) // <4>
      commandsTopic.publish(DecreaseValue(counterId, 1), counterId)

      val Message(incEvent, _) = eventsTopic.expectOneTyped[Increased] // <5>
      val Message(decEvent, _) = eventsTopic.expectOneTyped[Decreased]
      incEvent shouldBe Increased(4) // <6>
      decEvent shouldBe Decreased(1)
    }
    // end::test-topic[]

    // tag::test-topic-metadata[]
    "allow passing and reading metadata for messages" in {
      val increaseCmd = IncreaseValue(counterId, 4)
      val md = CloudEvent( // <1>
        id = "cmd1",
        source = URI.create("CounterServiceIntegrationSpec"),
        `type` = increaseCmd.companion.javaDescriptor.getFullName)
        .withSubject(counterId) // <2>
        .asMetadata
        .add("Content-Type", "application/protobuf"); // <3>

      commandsTopic.publish(Message(increaseCmd, md)) // <4>

      val Message(incEvent, actualMd) = eventsTopicWithMeta.expectOneTyped[Increased] // <5>
      incEvent shouldBe Increased(4)
      actualMd.get("Content-Type") should contain("application/protobuf") // <6>
      actualMd.asCloudEvent.subject should contain(counterId)
    }
    // end::test-topic-metadata[]
    // tag::test-topic[]
  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
// end::test-topic[]
