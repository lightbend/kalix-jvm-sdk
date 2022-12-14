package com.example.replicated.counter

import kalix.scalasdk.testkit.KalixTestKit
import com.example.replicated.Main
import com.example.replicated.counter.CounterService
import com.example.replicated.counter.DecreaseValue
import com.example.replicated.counter.GetValue
import com.example.replicated.counter.IncreaseValue
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

class SomeCounterIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()
  import testKit.executionContext

  private val counter = testKit.getGrpcClient(classOf[CounterService])

  "CounterService" must {

    "Increase and decrease a counter" in {
      val counterId = "42"

      val updateResult =
        for {
          _ <- counter.increase(IncreaseValue(counterId, 42))
          done <- counter.decrease(DecreaseValue(counterId, 32))
        } yield done

      updateResult.futureValue

      val getResult = counter.get(GetValue(counterId))
      getResult.futureValue.value shouldBe (42 - 32)
    }

    // FIXME remove test once TCK tests are updated to include replicated entities
    "Test value after passivation" in {
      val counterId = "1"

      counter.increase(IncreaseValue(counterId, 1)).futureValue
      Thread.sleep(5000)

      val getResult = counter.get(GetValue(counterId))
      getResult.futureValue.value shouldBe 1
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
