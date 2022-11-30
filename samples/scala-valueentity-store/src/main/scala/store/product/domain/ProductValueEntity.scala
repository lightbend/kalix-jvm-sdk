package store.product.domain

import com.google.protobuf.empty.Empty
import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext
import store.product.api

class ProductValueEntity(context: ValueEntityContext) extends AbstractProductValueEntity {

  override def emptyState: ProductState = ProductState.defaultInstance

  override def create(currentState: ProductState, product: api.Product): ValueEntity.Effect[Empty] = {
    val productState = ProductState(
      productId = product.productId,
      productName = product.productName,
      price = product.price.map(money => Money(money.currency, money.units, money.cents)))
    effects.updateState(productState).thenReply(Empty.defaultInstance)
  }

  override def get(currentState: ProductState, getProduct: api.GetProduct): ValueEntity.Effect[api.Product] = {
    val product = api.Product(
      productId = currentState.productId,
      productName = currentState.productName,
      price = currentState.price.map(money => api.Money(money.currency, money.units, money.cents)))
    effects.reply(product)
  }
}
