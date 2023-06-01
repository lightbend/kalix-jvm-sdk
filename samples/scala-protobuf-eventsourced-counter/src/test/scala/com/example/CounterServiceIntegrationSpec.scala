package com.example

import com.example.actions.{Decreased, Increased}
import kalix.scalasdk.testkit.{KalixTestKit, Message}
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

class CounterServiceIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()

  private val client = testKit.getGrpcClient(classOf[CounterService])

  private val outTopic = testKit.getTopic("counter-events")

  "CounterService" must {
    val counterId = "xyz"

    "handle side effect that adds the initial input multiplied by two and verify publishing" in {

      client.increaseWithSideEffect(IncreaseValue(counterId, 10)).futureValue
      val counter = client.getCurrentCounter(GetCounter(counterId)).futureValue
      counter.value shouldBe (10 + 10 * 2)

      // verify messages published to topic
      val allMsgs = testKit.getTopic("counter-events").expectN(2)

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
      val msg: Message[Decreased] = outTopic.expectOneTyped
      val Message(payload, md) = msg
      payload shouldBe Decreased(15)
      md.get("ce-type") should contain(classOf[Decreased].getName)
      md.get("Content-Type") should contain("application/protobuf")
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}