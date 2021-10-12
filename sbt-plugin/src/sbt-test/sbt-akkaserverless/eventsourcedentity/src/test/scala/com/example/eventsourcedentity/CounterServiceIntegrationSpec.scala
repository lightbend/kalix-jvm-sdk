package com.example.eventsourcedentity

import scala.concurrent.{ ExecutionContext, Future }

import akka.actor.ActorSystem
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.testkit.AkkaServerlessTestKit
import com.akkaserverless.scalasdk.testkit.EventSourcedResult
import com.google.protobuf.empty.Empty
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Tag
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

class CounterServiceIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  implicit val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  val testKit = AkkaServerlessTestKit(Main.createAkkaServerless())
  testKit.start()
  implicit val system: ActorSystem = testKit.system
  implicit val ec: ExecutionContext = system.dispatcher

  "CounterService" must {
    val client: CounterServiceClient =
      CounterServiceClient(testKit.grpcClientSettings)

    "Increase and decrease a timer" taggedAs(new Tag("it")) in {
      val counterId = "42"
      Future.sequence(Seq(
        client.increase(IncreaseValue(counterId, 42)),
        client.decrease(DecreaseValue(counterId, 32))
      )).futureValue
      val result = client.getCurrentCounter(GetCounter(counterId)).futureValue
      result.value shouldBe(42-32)
    }

  }

  override def afterAll() = {
    testKit.stop()
    super.afterAll()
  }
}
