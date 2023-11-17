//tag::RegisterComponents[]
package customer

import customer.action.CustomerActionImpl
import customer.domain.CustomerValueEntity
import customer.view.CustomerByEmailView
import customer.view.CustomerByNameView
import customer.view.CustomerDetailsByNameView
import customer.view.CustomerSummaryByNameView
import customer.view.CustomersResponseByCityView
import customer.view.CustomersResponseByNameView
import kalix.scalasdk.Kalix
import org.slf4j.LoggerFactory

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

object Main {

  private val log = LoggerFactory.getLogger("customer.Main")

  // tag::register[]
  def createKalix(): Kalix = {
    // end::register[]
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `Kalix()` instance.
    // tag::register[]
    KalixFactory.withComponents(
      new CustomerValueEntity(_),
      new CustomerActionImpl(_),
      new CustomerByEmailView(_),
      new CustomerByNameView(_),
      new CustomerDetailsByNameView(_),
      new CustomerSummaryByNameView(_),
      new CustomersResponseByCityView(_),
      new CustomersResponseByNameView(_))
  }
  // end::register[]

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
//end::RegisterComponents[]
