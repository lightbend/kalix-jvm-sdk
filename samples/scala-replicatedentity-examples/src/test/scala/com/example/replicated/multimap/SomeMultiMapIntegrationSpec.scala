package com.example.replicated.multimap

import kalix.scalasdk.testkit.KalixTestKit
import com.example.replicated.Main
import com.example.replicated.multimap.GetValues
import com.example.replicated.multimap.MultiMapService
import com.example.replicated.multimap.PutValue
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

class SomeMultiMapIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()
  import testKit.executionContext

  private val multiMapService = testKit.getGrpcClient(classOf[MultiMapService])

  "MultiMapService" must {
    "add and retrieve values from a multi-map" in {

      val mapId = "abc-def"
      val updateResult =
        for {
          _ <- multiMapService.put(PutValue(mapId, "foo", 10.0))
          _ <- multiMapService.put(PutValue(mapId, "foo", 15.0))
          _ <- multiMapService.put(PutValue(mapId, "bar", 20.0))
          _ <- multiMapService.put(PutValue(mapId, "bar", 10.0))
        } yield ()

      updateResult.futureValue

      val fooValues = multiMapService.get(GetValues(mapId, "foo")).futureValue
      fooValues.values shouldBe Seq(10.0, 15.0)

      val barValue = multiMapService.get(GetValues(mapId, "bar")).futureValue
      barValue.values shouldBe Seq(20.0, 10.0)
    }
  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
