package com.example.eventsourcedentity

import kalix.scalasdk.testkit.KalixTestKit
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

class CounterServiceIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()
  import testKit.executionContext

  private val client = testKit.getGrpcClient(classOf[CounterService])

  "CounterService" must {

    "Increase and decrease a counter" in {
      val counterId = "42"

      val updateResult =
        for {
          _ <- client.increase(IncreaseValue(counterId, 42))
          done <- client.decrease(DecreaseValue(counterId, 32))
        } yield done

      updateResult.futureValue

      val getResult = client.getCurrentCounter(GetCounter(counterId))
      getResult.futureValue.value shouldBe(42-32)
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
