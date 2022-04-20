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
    Kalix()
      .register(CustomerValueEntityProvider(new CustomerValueEntity(_)))
      .register(CustomerActionProvider(new CustomerActionImpl(_)))
      .register(CustomerByEmailViewProvider(new CustomerByEmailView(_)))
      .register(CustomerByNameViewProvider(new CustomerByNameView(_)))
      .register(CustomerSummaryByNameViewProvider(new CustomerSummaryByNameView(_)))
      .register(CustomersResponseByNameViewProvider(new CustomersResponseByNameView(_)))
  }
  // end::register[]

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
//end::RegisterComponents[]
