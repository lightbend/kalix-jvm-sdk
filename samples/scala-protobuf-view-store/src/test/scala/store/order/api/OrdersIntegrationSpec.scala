package store.order.api

import kalix.scalasdk.testkit.KalixTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec
import store.Main

class OrdersIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix(), KalixTestKit.DefaultSettings.withAdvancedViews()).start()

  private val orders = testKit.getGrpcClient(classOf[Orders])

  "Orders" must {

    "create and get order" in {
      val order = Order(orderId = "O1234", productId = "P123", customerId = "C001", quantity = 42)
      orders.create(order).futureValue
      orders.get(GetOrder("O1234")).futureValue shouldBe order
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
