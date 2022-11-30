package store.customer.api

import kalix.scalasdk.testkit.KalixTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec
import store.Main

class CustomersIntegrationSpec extends AnyWordSpec with Matchers with BeforeAndAfterAll with ScalaFutures {

  implicit private val patience: PatienceConfig = PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix(), KalixTestKit.DefaultSettings.withAdvancedViews()).start()

  private val customers = testKit.getGrpcClient(classOf[Customers])

  "Customers" must {

    "create and get customer" in {
      val customer = Customer(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(Address("123 Some Street", "Some City")))
      customers.create(customer).futureValue
      customers.get(GetCustomer("C001")).futureValue shouldBe customer
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
