package com.example.replicated.registermap

import kalix.scalasdk.testkit.KalixTestKit
import com.example.replicated.Main
import com.example.replicated.registermap.GetAllValues
import com.example.replicated.registermap.Key
import com.example.replicated.registermap.RegisterMapService
import com.example.replicated.registermap.SetValue
import com.example.replicated.registermap.Value
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

class SomeRegisterMapIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()
  import testKit.executionContext

  private val registerMapService = testKit.getGrpcClient(classOf[RegisterMapService])

  "RegisterMapService" must {
    "add and retrieve the value in a register by key" in {

      val FooKey = Key("foo")
      val BarKey = Key("bar")

      val registerId = "foobar"
      val updateResult =
        for {
          _ <- registerMapService.set(SetValue(registerId, key = Some(FooKey), value = Some(Value("foo"))))
          _ <- registerMapService.set(SetValue(registerId, key = Some(BarKey), value = Some(Value("bar"))))
        } yield ()
      updateResult.futureValue

      val state = registerMapService.getAll(GetAllValues(registerId)).futureValue
      state.values.map(_.value.get.field) shouldBe Seq("foo", "bar")

    }
  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
