package store.product.domain

import com.google.protobuf.empty.Empty
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import store.product.api

class ProductEntity(context: EventSourcedEntityContext) extends AbstractProductEntity {

  override def emptyState: ProductState = ProductState.defaultInstance

  override def create(currentState: ProductState, product: api.Product): EventSourcedEntity.Effect[Empty] = {
    val productState = ProductState(
      productId = product.productId,
      productName = product.productName,
      price = product.price.map(money => Money(money.currency, money.units, money.cents)))
    val productCreated = ProductCreated(Some(productState))
    effects.emitEvent(productCreated).thenReply(_ => Empty.defaultInstance)
  }

  override def get(currentState: ProductState, getProduct: api.GetProduct): EventSourcedEntity.Effect[api.Product] = {
    val product = api.Product(
      productId = currentState.productId,
      productName = currentState.productName,
      price = currentState.price.map(money => api.Money(money.currency, money.units, money.cents)))
    effects.reply(product)
  }

  override def changeName(
      currentState: ProductState,
      changeProductName: api.ChangeProductName): EventSourcedEntity.Effect[Empty] = {
    val productNameChanged = ProductNameChanged(newName = changeProductName.newName)
    effects.emitEvent(productNameChanged).thenReply(_ => Empty.defaultInstance)
  }

  override def changePrice(
      currentState: ProductState,
      changeProductPrice: api.ChangeProductPrice): EventSourcedEntity.Effect[Empty] = {
    val productPriceChanged = ProductPriceChanged(newPrice =
      changeProductPrice.newPrice.map(money => Money(money.currency, money.units, money.cents)))
    effects.emitEvent(productPriceChanged).thenReply(_ => Empty.defaultInstance)
  }

  override def productCreated(currentState: ProductState, productCreated: ProductCreated): ProductState =
    productCreated.product.get

  override def productNameChanged(currentState: ProductState, productNameChanged: ProductNameChanged): ProductState =
    currentState.copy(productName = productNameChanged.newName)

  override def productPriceChanged(currentState: ProductState, productPriceChanged: ProductPriceChanged): ProductState =
    currentState.copy(price = productPriceChanged.newPrice)

}
