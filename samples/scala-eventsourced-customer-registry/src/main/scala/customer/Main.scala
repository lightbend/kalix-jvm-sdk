package customer

import kalix.scalasdk.Kalix
import customer.domain.CustomerEntity
import customer.view.CustomerByCityStreamingView
import customer.view.CustomerByNameView
import org.slf4j.LoggerFactory

object Main {

  private val log = LoggerFactory.getLogger("customer.Main")

  // tag::register[]
  def createKalix(): Kalix = {
    KalixFactory.withComponents(
      new CustomerEntity(_),
      // end::register[]
      new CustomerByCityStreamingView(_),
      // tag::register[]
      new CustomerByNameView(_))
  }
  // end::register[]

  def main(args: Array[String]): Unit = {
    log.info("starting the Kalix service")
    createKalix().start()
  }
}
