package com.example.replicated.map

import kalix.scalasdk.testkit.KalixTestKit
import com.example.replicated.Main
import com.example.replicated.map.AddBazValue
import com.example.replicated.map.GetValues
import com.example.replicated.map.IncreaseFooValue
import com.example.replicated.map.MapService
import com.example.replicated.map.SetBarValue
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

class SomeMapIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()
  import testKit.executionContext

  private val mapService = testKit.getGrpcClient(classOf[MapService])

  "MapService" must {

    "Add and retrieve values from map" in {
      val mapId = "foobar"

      val updateResult =
        for {
          _ <- mapService.increaseFoo(IncreaseFooValue(mapId, 10))
          _ <- mapService.setBar(SetBarValue(mapId, "abc"))
          _ <- mapService.addBaz(AddBazValue(mapId, "def"))
          _ <- mapService.addBaz(AddBazValue(mapId, "ghi"))
        } yield ()

      updateResult.futureValue

      val mapState = mapService.get(GetValues(mapId)).futureValue
      mapState.foo shouldBe 10
      mapState.bar shouldBe "abc"
      mapState.baz shouldBe Seq("def", "ghi")
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
