package com.example.replicated.countermap

import kalix.scalasdk.testkit.KalixTestKit
import com.example.replicated.Main
import com.example.replicated.countermap.CounterMapService
import com.example.replicated.countermap.DecreaseValue
import com.example.replicated.countermap.GetValue
import com.example.replicated.countermap.IncreaseValue
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

class SomeCounterMapIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()
  import testKit.executionContext

  private val counterMap = testKit.getGrpcClient(classOf[CounterMapService])

  "CounterMapService" must {

    "Increase and decrease a counter by key" in {

      val counterId = "42"

      { // update foo

        val updateResult =
          for {
            _ <- counterMap.increase(IncreaseValue(counterId, "foo", 42))
            done <- counterMap.decrease(DecreaseValue(counterId, "foo", 32))
          } yield done

        updateResult.futureValue

        val getResult = counterMap.get(GetValue(counterId, "foo"))
        getResult.futureValue.value shouldBe (42 - 32)
      }

      { // update bar

        val updateResult =
          for {
            _ <- counterMap.increase(IncreaseValue(counterId, "bar", 442))
            done <- counterMap.decrease(DecreaseValue(counterId, "bar", 332))
          } yield done

        updateResult.futureValue

        val getResult = counterMap.get(GetValue(counterId, "bar"))
        getResult.futureValue.value shouldBe (442 - 332)
      }
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
