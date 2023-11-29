package customer.view

import customer.Main
import customer.domain.Address
import customer.domain.CustomerState
import kalix.scalasdk.testkit.KalixTestKit
import kalix.scalasdk.testkit.KalixTestKit.DefaultSettings
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

// tag::view-test[]
class CustomersResponseByCityViewIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {
  // end::view-test[]

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  // tag::view-test[]
  private val testKit =
    KalixTestKit(Main.createKalix(), DefaultSettings.withValueEntityIncomingMessages("customer")) // <1>
      .start()

  private val viewClient = testKit.getGrpcClient(classOf[CustomersResponseByCity])

  "CustomersResponseByCityView" should {

    "find customers by city" in {
      val customerEvents = testKit.getValueEntityIncomingMessages("customer") // <2>

      val johanna = CustomerState("1", "johanna@example.com", "Johanna", Some(Address("Cool Street", "Porto")))
      val bob = CustomerState("2", "bob@example.com", "Bob", Some(Address("Baker Street", "London")))
      val alice = CustomerState("3", "alice@example.com", "Alice", Some(Address("Long Street", "Wroclaw")))

      customerEvents.publish(johanna, "1") // <3>
      customerEvents.publish(bob, "2")
      customerEvents.publish(alice, "3")

      eventually {

        val customersResponse = viewClient
          .getCustomers(ByCityRequest(Seq("Porto", "London"))) // <4>
          .futureValue

        customersResponse.customers should contain only (johanna, bob)
      }
    }
  }
  // end::view-test[]

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
  // tag::view-test[]
}
// end::view-test[]
