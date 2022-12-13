package store.view.joined

import akka.stream.scaladsl.Sink
import kalix.scalasdk.testkit.KalixTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.{ Millis, Seconds, Span }
import org.scalatest.wordspec.AnyWordSpec
import store.Main
import store.customer.api.{ Address, ChangeCustomerName, Customer, Customers }
import store.order.api.{ Order, Orders }
import store.product.api.{ ChangeProductName, Money, Product, Products }

class JoinedCustomerOrdersIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {

  implicit private val patience: PatienceConfig = PatienceConfig(Span(5, Seconds), Span(500, Millis))

  // tag::testkit-advanced-views[]
  private val testKit = KalixTestKit(Main.createKalix(), KalixTestKit.DefaultSettings.withAdvancedViews()).start()
  // end::testkit-advanced-views[]

  private val products = testKit.getGrpcClient(classOf[Products])
  private val customers = testKit.getGrpcClient(classOf[Customers])
  private val orders = testKit.getGrpcClient(classOf[Orders])
  private val view = testKit.getGrpcClient(classOf[JoinedCustomerOrders])

  private def getCustomerOrders(customerId: String): Seq[CustomerOrder] = {
    view.get(CustomerOrdersRequest("C001")).runWith(Sink.seq)(testKit.materializer).futureValue
  }

  "JoinedCustomerOrders" must {

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
      val results = eventually {
        val customerOrders = getCustomerOrders("C001")
        customerOrders.size should be >= 2
        customerOrders
      }

      results should have size 2

      val expectedOrder1 = CustomerOrder(
        orderId = "O1234",
        productId = "P123",
        productName = "Super Duper Thingamajig",
        price = Some(store.product.domain.Money("USD", 123, 45)),
        quantity = 42,
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(store.customer.domain.Address("123 Some Street", "Some City")),
        created = results.head.created)

      val expectedOrder2 = CustomerOrder(
        orderId = "O5678",
        productId = "P987",
        productName = "Awesome Whatchamacallit",
        price = Some(store.product.domain.Money("NZD", 987, 65)),
        quantity = 7,
        customerId = "C001",
        email = "someone@example.com",
        name = "Some Customer",
        address = Some(store.customer.domain.Address("123 Some Street", "Some City")),
        created = results(1).created)

      results shouldBe Seq(expectedOrder1, expectedOrder2)

      val newCustomerName = "Some Name"
      val changeCustomerName = ChangeCustomerName(customerId = "C001", newName = newCustomerName)
      customers.changeName(changeCustomerName).futureValue

      // wait until the view is eventually updated
      val resultsWithNewCustomerName = eventually {
        val customerOrders = getCustomerOrders("C001")
        customerOrders.head.name shouldBe newCustomerName
        customerOrders
      }

      val expectedOrder1WithNewCustomerName = expectedOrder1.withName(newCustomerName)
      val expectedOrder2WithNewCustomerName = expectedOrder2.withName(newCustomerName)

      resultsWithNewCustomerName shouldBe Seq(expectedOrder1WithNewCustomerName, expectedOrder2WithNewCustomerName)

      val newProductName = "Thing Supreme"
      val changeProductName = ChangeProductName(productId = "P123", newName = newProductName)
      products.changeName(changeProductName).futureValue

      // wait until the view is eventually updated
      val resultsWithNewProductName = eventually {
        val customerOrders = getCustomerOrders("C001")
        customerOrders.head.productName shouldBe newProductName
        customerOrders
      }

      val expectedOrder1WithNewProductName = expectedOrder1WithNewCustomerName.withProductName(newProductName)

      resultsWithNewProductName shouldBe Seq(expectedOrder1WithNewProductName, expectedOrder2WithNewCustomerName)
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
