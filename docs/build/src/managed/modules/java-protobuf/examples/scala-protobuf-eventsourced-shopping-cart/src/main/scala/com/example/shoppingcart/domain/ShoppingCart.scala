package com.example.shoppingcart.domain

// tag::imports[]
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityContext
import com.example.shoppingcart
import com.google.protobuf.empty.Empty
import scala.annotation.nowarn

import com.example.shoppingcart.CheckoutShoppingCart
// end::imports[]

// tag::class[]
class ShoppingCart(context: EventSourcedEntityContext) extends AbstractShoppingCart { // <1>

  @nowarn("msg=unused")
  private val entityId = context.entityId

  override def emptyState: Cart = Cart.defaultInstance // <2>
  // end::class[]

  // tag::addItem[]
  override def addItem(currentState: Cart, addLineItem: shoppingcart.AddLineItem): EventSourcedEntity.Effect[Empty] =
    if (currentState.checkedOut)
      effects.error("Cart is already checked out")
    else if (addLineItem.quantity <= 0)
      effects.error(s"Quantity for item ${addLineItem.productId} must be greater than zero.") // <1>
    else {
      val event = ItemAdded( // <2>
        Some(LineItem(productId = addLineItem.productId, name = addLineItem.name, quantity = addLineItem.quantity)))
      effects
        .emitEvent(event) // <3>
        .thenReply(_ => Empty.defaultInstance) // <4>
    }
  // end::addItem[]

  // tag::removeItem[]
  override def removeItem(
      currentState: Cart,
      removeLineItem: shoppingcart.RemoveLineItem): EventSourcedEntity.Effect[Empty] =
    if (currentState.checkedOut)
      effects.error("Cart is already checked out")
    else if (!currentState.items.exists(_.productId == removeLineItem.productId)) {
      effects.error(s"Cannot remove item ${removeLineItem.productId} because it is not in the cart.")
    } else {
      val event = ItemRemoved(removeLineItem.productId)
      effects
        .emitEvent(event)
        .thenReply(_ => Empty.defaultInstance)
    }
  // end::removeItem[]

  // tag::getCart[]
  override def getCart(
      currentState: Cart, // <1>
      getShoppingCart: shoppingcart.GetShoppingCart): EventSourcedEntity.Effect[shoppingcart.Cart] = {
    val apiItems = currentState.items.map(convertToApi).sortBy(_.productId)
    val apiCart = shoppingcart.Cart(apiItems, currentState.checkedOut) // <2>
    effects.reply(apiCart)
  }

  private def convertToApi(item: LineItem): shoppingcart.LineItem =
    shoppingcart.LineItem(productId = item.productId, name = item.name, quantity = item.quantity)
  // end::getCart[]

  // tag::checkout[]
  override def checkout(
      currentState: Cart,
      checkoutShoppingCart: CheckoutShoppingCart): EventSourcedEntity.Effect[Empty] = {
    if (currentState.checkedOut)
      effects.error("Cart is already checked out")
    else
      effects
        .emitEvent(CheckedOut.defaultInstance) // <1>
        .deleteEntity() // <2>
        .thenReply(_ => Empty.defaultInstance);
  }
  // end::checkout[]

  // tag::itemAdded[]
  override def itemAdded(currentState: Cart, itemAdded: ItemAdded): Cart = {
    val cart = currentState.items.map(lineItem => lineItem.productId -> lineItem).toMap
    val item = cart.get(itemAdded.getItem.productId) match {
      case Some(existing) => existing.copy(quantity = existing.quantity + itemAdded.getItem.quantity)
      case None           => itemAdded.getItem
    }
    val updatedCart = cart + (item.productId -> item)
    currentState.withItems(updatedCart.values.toSeq)
  }
  // end::itemAdded[]

  // tag::itemRemoved[]
  override def itemRemoved(currentState: Cart, itemRemoved: ItemRemoved): Cart =
    currentState.withItems(currentState.items.filterNot(_.productId == itemRemoved.productId))
  // end::itemRemoved[]

  override def checkedOut(currentState: Cart, checkedOut: CheckedOut): Cart =
    currentState.copy(checkedOut = true)
}
