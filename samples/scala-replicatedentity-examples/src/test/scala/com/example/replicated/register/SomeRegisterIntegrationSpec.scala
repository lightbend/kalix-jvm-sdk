package com.example.replicated.register

import kalix.scalasdk.testkit.KalixTestKit
import com.example.replicated.Main
import com.example.replicated.register.GetValue
import com.example.replicated.register.RegisterService
import com.example.replicated.register.SetValue
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

class SomeRegisterIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()

  private val registerService = testKit.getGrpcClient(classOf[RegisterService])

  "RegisterService" must {
    "add and retrieve the value in a register" in {

      val registerId = "abc-def"
      val updateResult = registerService.set(SetValue(registerId, "foo"))
      updateResult.futureValue

      val state = registerService.get(GetValue(registerId)).futureValue
      state.value shouldBe "foo"

    }
  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
