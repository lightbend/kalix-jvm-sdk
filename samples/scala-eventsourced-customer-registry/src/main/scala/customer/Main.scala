package customer

import com.akkaserverless.scalasdk.AkkaServerless
import customer.domain.CustomerEntity
import customer.view.CustomerByNameView
import org.slf4j.LoggerFactory

object Main {

  private val log = LoggerFactory.getLogger("customer.Main")

  // tag::register[]
  def createAkkaServerless(): AkkaServerless = {
    AkkaServerlessFactory.withComponents(
      new CustomerEntity(_),
      new CustomerByNameView(_))
  }
  // end::register[]

  def main(args: Array[String]): Unit = {
    log.info("starting the Akka Serverless service")
    createAkkaServerless().start()
  }
}
