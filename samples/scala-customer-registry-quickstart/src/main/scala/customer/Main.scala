package customer

import kalix.scalasdk.AkkaServerless
import customer.domain.Customer
import customer.view.CustomerByEmailView
import customer.view.CustomerByNameView
import org.slf4j.LoggerFactory

object Main {

  private val log = LoggerFactory.getLogger("customer.Main")

  def createAkkaServerless(): AkkaServerless = {
    // The AkkaServerlessFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `AkkaServerless()` instance.
    AkkaServerlessFactory.withComponents(
      new Customer(_),
      new CustomerByEmailView(_),
      new CustomerByNameView(_))
  }

  def main(args: Array[String]): Unit = {
    log.info("starting the Akka Serverless service")
    createAkkaServerless().start()
  }
}
