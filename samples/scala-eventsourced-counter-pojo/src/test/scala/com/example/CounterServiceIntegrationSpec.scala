package com.example

import akka.actor.ActorSystem
import com.akkaserverless.scalasdk.testkit.AkkaServerlessTestKit
import com.google.protobuf.empty.Empty
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class CounterServiceIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = AkkaServerlessTestKit(Main.createAkkaServerless()).start()

  private val client = testKit.getGrpcClient(classOf[CounterService])

  "CounterService" must {

    "load, crash and reload entity" in {
      val counterId = "abc"
      client.increase(IncreaseValue(counterId, 10)).futureValue

      // going below 0 will make it crash
      client.decrease(DecreaseValue(counterId, 11)).failed.futureValue

      { // loading it again to confirm serialization is properly wired
        val counter = client.getCurrentCounter(GetCounter(counterId)).futureValue
        counter.value shouldBe (10)
      }

      client.reset(ResetValue(counterId)).futureValue
      // crash it again and reload
      client.decrease(DecreaseValue(counterId, 11)).failed.futureValue

      { // reload to confirm that expecial serialization of object ValueReset is handled propertly
        val counter = client.getCurrentCounter(GetCounter(counterId)).futureValue
        counter.value shouldBe (0)
      }
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}