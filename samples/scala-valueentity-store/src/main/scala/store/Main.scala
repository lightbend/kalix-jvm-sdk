package store

import kalix.scalasdk.Kalix
import org.slf4j.LoggerFactory
import store.customer.domain.CustomerValueEntity
import store.order.domain.OrderValueEntity
import store.product.domain.ProductValueEntity
import store.view.joined.JoinedCustomerOrdersView
import store.view.nested.NestedCustomerOrdersView
import store.view.structured.StructuredCustomerOrdersView

object Main {

  private val log = LoggerFactory.getLogger("store.Main")

  def createKalix(): Kalix = {
    KalixFactory.withComponents(
      new CustomerValueEntity(_),
      new OrderValueEntity(_),
      new ProductValueEntity(_),
      new JoinedCustomerOrdersView(_),
      new NestedCustomerOrdersView(_),
      new StructuredCustomerOrdersView(_))
  }

  def main(args: Array[String]): Unit = {
    log.info("Starting the Kalix store sample")
    createKalix().start()
  }
}
