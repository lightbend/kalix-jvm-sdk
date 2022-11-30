package store.view.structured

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

class StructuredCustomerOrdersIntegrationSpec
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
  private val view = testKit.getGrpcClient(classOf[StructuredCustomerOrders])

  private def getCustomerOrders(customerId: String): CustomerOrders = {
    view.get(CustomerOrdersRequest("C001")).futureValue
  }

  "StructuredCustomerOrders" must {

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
        id = "C001",
        shipping = Some(
          CustomerShipping(
            name = "Some Customer",
            address1 = "123 Some Street",
            address2 = "Some City",
            contactEmail = "someone@example.com")),
        orders = Seq(
          ProductOrder(
            id = "P123",
            name = "Super Duper Thingamajig",
            quantity = 42,
            value = Some(ProductValue("USD", 123, 45)),
            orderId = "O1234",
            orderCreated = result.orders.head.orderCreated),
          ProductOrder(
            id = "P987",
            name = "Awesome Whatchamacallit",
            quantity = 7,
            value = Some(ProductValue("NZD", 987, 65)),
            orderId = "O5678",
            orderCreated = result.orders(1).orderCreated)))
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
