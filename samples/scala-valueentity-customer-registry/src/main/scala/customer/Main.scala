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
    // FIXME temporarily changed to set a short view id to not hit view id limit of 21 chars
    new Kalix()
      .register(CustomerValueEntityProvider.of(new CustomerValueEntity(_)))
      .register(CustomerActionImplProvider.of(new CustomerActionImpl(_)))
      .register(CustomerByEmailViewProvider.of(new CustomerByEmailView(_)).withViewId("ByEmail"))
      .register(CustomerByNameViewProvider.of(new CustomerByNameView(_)).withViewId("ByName"))
      .register(CustomerSummaryByNameViewProvider.of(new CustomerSummaryByNameView(_))
        .withViewId("Summary"))
      .register(CustomersResponseByNameViewProvider.of(new CustomersResponseByNameView(_))
        .withViewId("Response"))
  }
  // end::register[]

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
//end::RegisterComponents[]
