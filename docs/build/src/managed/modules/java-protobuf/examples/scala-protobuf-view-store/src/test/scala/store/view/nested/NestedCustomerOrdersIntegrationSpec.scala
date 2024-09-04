package store.view.nested

import kalix.scalasdk.testkit.KalixTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec
import store.Main
import store.customer.api.{ Address, Customer, Customers }
import store.order.api.{ Order, Orders }
import store.product.api.{ Money, Product, Products }

class NestedCustomerOrdersIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {

  implicit private val patience: PatienceConfig = PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix(), KalixTestKit.DefaultSettings.withAdvancedViews()).start()

  private val products = testKit.getGrpcClient(classOf[Products])
  private val customers = testKit.getGrpcClient(classOf[Customers])
  private val orders = testKit.getGrpcClient(classOf[Orders])
  private val view = testKit.getGrpcClient(classOf[NestedCustomerOrders])

  private def getCustomerOrders(customerId: String): CustomerOrders = {
    view.get(CustomerOrdersRequest("C001")).futureValue
  }

  "NestedCustomerOrders" must {

    "create and get customer" in {
      val product1 =
        Product(productId = "P123", productName = "Super Duper Thingamajig", price = Some(Money("USD", 123, 45)))
      products.create(product1).futureValue
      val product2 =
        Product(productId = "P987", productName = "Awesome Whatchamacallit", price = Some(Money("NZD", 987, 65)))
      products.create(product2).futureValue
      val customer = Customer(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(Address("123 Some Street", "Some City")))
      customers.create(customer).futureValue
      val order1 = Order(orderId = "O1234", productId = "P123", customerId = "C001", quantity = 42)
      orders.create(order1).futureValue
      val order2 = Order(orderId = "O5678", productId = "P987", customerId = "C001", quantity = 7)
      orders.create(order2).futureValue

      // wait until the view is eventually updated
      val result = eventually {
        val customerOrders = getCustomerOrders("C001")
        customerOrders.orders.size should be >= 2
        customerOrders
      }

      result.orders should have size 2

      result shouldBe CustomerOrders(
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(store.customer.domain.Address("123 Some Street", "Some City")),
        orders = Seq(
          CustomerOrder(
            customerId = "C001",
            orderId = "O1234",
            productId = "P123",
            productName = "Super Duper Thingamajig",
            price = Some(store.product.domain.Money("USD", 123, 45)),
            quantity = 42,
            created = result.orders.head.created),
          CustomerOrder(
            customerId = "C001",
            orderId = "O5678",
            productId = "P987",
            productName = "Awesome Whatchamacallit",
            price = Some(store.product.domain.Money("NZD", 987, 65)),
            quantity = 7,
            created = result.orders(1).created)))
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
