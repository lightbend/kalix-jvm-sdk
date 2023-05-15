package customer

import java.util.UUID

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters.MapHasAsJava

import akka.actor.ActorSystem
import akka.stream.SystemMaterializer
import akka.stream.scaladsl.Sink
import com.typesafe.config.ConfigFactory
import customer.action.CustomerAction
import customer.api.Customer
import customer.view.AllCustomersView
import kalix.devtools.impl.DockerComposeUtils
import kalix.javasdk.impl.GrpcClients
import kalix.scalasdk.KalixRunner
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import com.google.protobuf.empty.Empty
import org.scalatest.time.Minutes
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

class CustomerServiceIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {

  // need to be very patient because we need time to bootstrap
  // two proxies and have then connecting to their respective user functions
  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Minutes), Span(5, Seconds))

  private val testSystem = ActorSystem("test-system")
  private val dockerComposeUtils = DockerComposeUtils("docker-compose-integration.yml")

  private val kalixRunner: KalixRunner = {
    val confMap = Map(
      "kalix.user-function-port" -> dockerComposeUtils.userFunctionPort.toString,
      // don't kill the test JVM when terminating the KalixRunner// don't kill the test JVM when terminating the KalixRunner
      "kalix.system.akka.coordinated-shutdown.exit-jvm" -> "off",
      // dev-mode should be false when running integration tests// dev-mode should be false when running integration tests
      "kalix.dev-mode.enabled" -> "false",
      "kalix.user-function-interface" -> "0.0.0.0")

    val finalMap =
      dockerComposeUtils.localServicePortMappings.foldLeft(confMap) { case (map, item) =>
        val split = item.replace("-D", "").split("=")
        map + (split(0) -> split(1))
      }

    val config = ConfigFactory.parseMap(finalMap.asJava).withFallback(ConfigFactory.load())
    Main.createKalix().createRunner(config)

  }

  override def beforeAll(): Unit = {
    dockerComposeUtils.start()
    kalixRunner.run()
  }

  override def afterAll(): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    val allStopped =
      Future.sequence(Seq(testSystem.terminate(), kalixRunner.terminate(), Future(dockerComposeUtils.stopAndWait())))
    Await.result(allStopped, 5.seconds)
  }

  def customerActionClient: CustomerAction =
    GrpcClients.get(testSystem).getGrpcClient(classOf[CustomerAction], "localhost", 9001)

  def customersViewClient: AllCustomersView =
    GrpcClients.get(testSystem).getGrpcClient(classOf[AllCustomersView], "localhost", 9001)

  "CustomerService" must {

    "create a customer on customer-registry and receive it back via stream to stream" in {
      val id = UUID.randomUUID().toString

      eventually {
        val ack =
          customerActionClient.create(Customer(customerId = id, email = "foo@example.com", name = "Johanna"))
        ack.futureValue shouldBe Empty.defaultInstance
      }

      eventually {
        val customer =
          customersViewClient
            .getCustomers(Empty.defaultInstance)
            .runWith(Sink.last)(SystemMaterializer(testSystem).materializer)
            .futureValue
        customer.customerId shouldBe id
      }
    }

  }

}
