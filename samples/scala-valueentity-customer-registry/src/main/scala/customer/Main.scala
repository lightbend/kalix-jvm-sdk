//tag::RegisterComponents[]
package customer

import kalix.scalasdk.Kalix
import customer.action._
import customer.domain._
import customer.view._
import org.slf4j.LoggerFactory

object Main {

  private val log = LoggerFactory.getLogger("customer.Main")

  // tag::register[]
  def createKalix(): Kalix = {
    // FIXME temporarily changed to set a short view id to not hit view id limit of 21 chars
    Kalix()
      .register(CustomerValueEntityProvider(new CustomerValueEntity(_)))
      .register(CustomerActionProvider(new CustomerActionImpl(_)))
      .register(CustomerByEmailViewProvider(new CustomerByEmailView(_)).withViewId("ByEmail"))
      .register(CustomerByNameViewProvider(new CustomerByNameView(_)).withViewId("ByName"))
      .register(CustomerSummaryByNameViewProvider(new CustomerSummaryByNameView(_))
        .withViewId("Summary"))
      .register(CustomersResponseByNameViewProvider(new CustomersResponseByNameView(_))
        .withViewId("Response"))
  }
  // end::register[]

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
//end::RegisterComponents[]
