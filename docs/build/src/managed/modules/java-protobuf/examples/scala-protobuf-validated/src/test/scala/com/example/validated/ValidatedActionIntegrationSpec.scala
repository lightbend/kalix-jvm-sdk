package com.example.validated

import akka.grpc.GrpcServiceException
import kalix.scalasdk.testkit.KalixTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ValidatedActionIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig = PatienceConfig(5.seconds, 500.millis)

  private val testKit = KalixTestKit(Main.createKalix()).start()

  private val client = testKit.getGrpcClient(classOf[Validated])

  "ValidatedAction" must {

    "handle command CallMeMaybe" in {
      val result = client.callMeMaybe(Request("valid.email@example.com")).futureValue
      result should ===(Response("Hello valid.email@example.com, gosh what a valid email you sent me"))

      // we can't test validation failure because we can't construct an invalid request
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
