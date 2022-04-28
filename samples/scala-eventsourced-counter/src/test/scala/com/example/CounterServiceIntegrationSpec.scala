package com.example

import akka.actor.ActorSystem
import kalix.scalasdk.testkit.KalixTestKit
import com.google.protobuf.empty.Empty
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CounterServiceIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()

  private val client = testKit.getGrpcClient(classOf[CounterService])

  "CounterService" must {

    "handle side effect that adds the initial input multiplied by two" in {
      val counterId = "xyz"

      client.increaseWithSideEffect(IncreaseValue(counterId, 42)).futureValue

      val counter = client.getCurrentCounter(GetCounter(counterId)).futureValue

      counter.value shouldBe (42 + 42 * 2)
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}