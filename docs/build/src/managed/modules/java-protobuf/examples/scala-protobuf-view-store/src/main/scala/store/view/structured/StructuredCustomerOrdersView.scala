package store.view.structured

import kalix.scalasdk.view.View.UpdateEffect
import kalix.scalasdk.view.ViewContext
import store.customer.domain.CustomerAddressChanged
import store.customer.domain.CustomerCreated
import store.customer.domain.CustomerNameChanged
import store.customer.domain.CustomerState
import store.product.domain.ProductCreated
import store.product.domain.ProductNameChanged
import store.product.domain.ProductPriceChanged
import store.product.domain.ProductState

class StructuredCustomerOrdersView(context: ViewContext) extends AbstractStructuredCustomerOrdersView {

  object CustomersViewTable extends AbstractCustomersViewTable {

    override def emptyState: CustomerState = CustomerState.defaultInstance

    override def processCustomerCreated(
        state: CustomerState,
        customerCreated: CustomerCreated): UpdateEffect[CustomerState] =
      if (state != emptyState) effects.ignore() // already created
      else effects.updateState(customerCreated.customer.get)

    override def processCustomerNameChanged(
        state: CustomerState,
        customerNameChanged: CustomerNameChanged): UpdateEffect[CustomerState] =
      effects.updateState(state.copy(name = customerNameChanged.newName))

    override def processCustomerAddressChanged(
        state: CustomerState,
        customerAddressChanged: CustomerAddressChanged): UpdateEffect[CustomerState] =
      effects.updateState(state.copy(address = customerAddressChanged.newAddress))

  }

  object ProductsViewTable extends AbstractProductsViewTable {

    override def emptyState: ProductState = ProductState.defaultInstance

    override def processProductCreated(
        state: ProductState,
        productCreated: ProductCreated): UpdateEffect[ProductState] =
      if (state != emptyState) effects.ignore() // already created
      else effects.updateState(productCreated.product.get)

    override def processProductNameChanged(
        state: ProductState,
        productNameChanged: ProductNameChanged): UpdateEffect[ProductState] =
      effects.updateState(state.copy(productName = productNameChanged.newName))

    override def processProductPriceChanged(
        state: ProductState,
        productPriceChanged: ProductPriceChanged): UpdateEffect[ProductState] =
      effects.updateState(state.copy(price = productPriceChanged.newPrice))

  }

}
