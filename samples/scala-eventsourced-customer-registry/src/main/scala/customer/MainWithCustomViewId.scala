package customer

import com.akkaserverless.scalasdk.AkkaServerless
import customer.domain.CustomerEntity
import customer.domain.CustomerEntityProvider
import customer.view.CustomerByNameView
import customer.view.CustomerByNameViewProvider
import org.slf4j.LoggerFactory

object MainWithCustomViewId {

  private val log = LoggerFactory.getLogger("customer.Main")

  // tag::register[]
  def createAkkaServerless(): AkkaServerless =
    AkkaServerless()
      .register(
        CustomerByNameViewProvider(new CustomerByNameView(_))
          .withViewId("CustomerByNameV2"))
      .register(
        CustomerEntityProvider(new CustomerEntity(_))
      )
  // end::register[]

  def main(args: Array[String]): Unit = {
    log.info("starting the Akka Serverless service")
    createAkkaServerless().start()
  }
}
