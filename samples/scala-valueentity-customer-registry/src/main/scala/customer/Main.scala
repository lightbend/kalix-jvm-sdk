//tag::RegisterComponents[]
package customer

import kalix.scalasdk.Kalix
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
  def createKalix(): Kalix = {
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `Kalix()` instance.
    KalixFactory.withComponents(
      // end::register[]
      new CustomerValueEntity(_),
      new CustomerActionImpl(_),
      new CustomerByEmailView(_),
      new CustomerByNameView(_),
      new CustomerSummaryByNameView(_),
      new CustomersResponseByNameView(_))
      /*
      // tag::register[]
      return KalixFactory.withComponents(
        new CustomerValueEntity(_),
        new CustomerByNameView(_))
      // end::register[]
      */
    // tag::register[]
  }
  // end::register[]

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
//end::RegisterComponents[]
