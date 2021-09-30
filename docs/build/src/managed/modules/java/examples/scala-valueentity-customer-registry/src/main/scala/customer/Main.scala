//tag::RegisterComponents[]
package customer

import com.akkaserverless.scalasdk.AkkaServerless
import customer.action.CustomerActionImpl
import customer.domain.CustomerValueEntity
import customer.view.CustomerByEmailView
import customer.view.CustomerByNameView
import customer.view.CustomerSummaryByNameView
import customer.view.CustomersResponseByNameView
import org.slf4j.LoggerFactory

object Main {

  private val log = LoggerFactory.getLogger("customer.Main")

  // tag::register[]
  def createAkkaServerless(): AkkaServerless = {
    // The AkkaServerlessFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `AkkaServerless()` instance.
    AkkaServerlessFactory.withComponents(
      // end::register[]
      new CustomerValueEntity(_),
      new CustomerActionImpl(_),
      new CustomerByEmailView(_),
      new CustomerByNameView(_),
      new CustomerSummaryByNameView(_),
      new CustomersResponseByNameView(_))
      /*
      // tag::register[]
      return AkkaServerlessFactory.withComponents(
        new CustomerValueEntity(_),
        new CustomerByNameView(_))
      // end::register[]
      */
    // tag::register[]
  }
  // end::register[]

  def main(args: Array[String]): Unit = {
    log.info("starting the Akka Serverless service")
    createAkkaServerless().start()
  }
}
//end::RegisterComponents[]
