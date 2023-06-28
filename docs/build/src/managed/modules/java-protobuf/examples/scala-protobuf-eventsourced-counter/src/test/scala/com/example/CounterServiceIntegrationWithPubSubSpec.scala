package com.example

import com.example.actions.{Decreased, Increased}
import kalix.scalasdk.CloudEvent
import kalix.scalasdk.testkit.KalixTestKit.DefaultSettings
import kalix.scalasdk.testkit.KalixTestKit.Settings.GooglePubSubEmulator

import java.net.URI
// tag::test-topic[]
import kalix.scalasdk.testkit.{KalixTestKit, Message}
import org.scalatest.BeforeAndAfterEach
// ...
// end::test-topic[]
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.wordspec.AnyWordSpec

import scala.language.postfixOps

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::test-topic[]

class CounterServiceIntegrationWithPubSubSpec extends AnyWordSpec with Matchers with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {

  // end::test-topic[]
  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  // tag::test-topic[]
  private val testKit = KalixTestKit(Main.createKalix(), DefaultSettings.withEventingSupport(GooglePubSubEmulator)).start() // <1>
  // end::test-topic[]

  private val client = testKit.getGrpcClient(classOf[CounterService])

  // tag::test-topic[]

  "CounterService" must {
    val counterId = "xyz"
    // end::test-topic[]

    "handle side effect that adds the initial input multiplied by two and verify publishing" in {

      client.increaseWithSideEffect(IncreaseValue(counterId, 10)).futureValue
      val counter = client.getCurrentCounter(GetCounter(counterId)).futureValue
      counter.value shouldBe (10 + 10 * 2)
    }
  }


  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
// end::test-topic[]
