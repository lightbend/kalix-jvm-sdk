package com.example.replicated.set

import kalix.scalasdk.testkit.KalixTestKit
import com.example.replicated.Main
import com.example.replicated.set.AddElement
import com.example.replicated.set.GetElements
import com.example.replicated.set.RemoveElement
import com.example.replicated.set.SetService
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

class SomeSetIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()
  import testKit.executionContext

  private val setService = testKit.getGrpcClient(classOf[SetService])

  "SetService" must {
    "add and retrieve the value in a register by key" in {

      val setId = "foobar"
      val updateResult =
        for {
          _ <- setService.add(AddElement(setId, "foo"))
          _ <- setService.add(AddElement(setId, "baz"))
          _ <- setService.add(AddElement(setId, "bar"))
          _ <- setService.remove(RemoveElement(setId, "baz"))
        } yield ()
      updateResult.futureValue

      val state = setService.get(GetElements(setId)).futureValue
      state.elements.sorted shouldBe Seq("bar", "foo")

    }
  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
