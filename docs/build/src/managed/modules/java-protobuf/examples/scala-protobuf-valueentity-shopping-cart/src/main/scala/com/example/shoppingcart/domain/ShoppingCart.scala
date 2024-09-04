package com.example.shoppingcart.domain

import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext
import com.example.shoppingcart
import com.google.protobuf.empty.Empty

import java.time.Instant

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class ShoppingCart(context: ValueEntityContext) extends AbstractShoppingCart {

  override def emptyState: Cart = Cart.defaultInstance

  // tag::create[]
  override def create(currentState: Cart, createCart: shoppingcart.CreateCart): ValueEntity.Effect[Empty] =
    if (currentState.creationTimestamp > 0)
      effects.error("Cart was already created")
    else
      effects.updateState(currentState.copy(creationTimestamp = Instant.now().toEpochMilli))
        .thenReply(Empty.defaultInstance)
  // end::create[]

  // tag::add-item[]
  override def addItem(currentState: Cart, addLineItem: shoppingcart.AddLineItem): ValueEntity.Effect[Empty] =
    if (addLineItem.quantity <= 0)
      effects.error(s"Quantity for item ${addLineItem.quantity} must be greater than zero")
    else {
      val cart = currentState.items.map(lineItem => lineItem.productId -> lineItem).toMap
      val item = cart.get(addLineItem.productId) match {
        case Some(existing) =>
          existing.copy(quantity = existing.quantity + addLineItem.quantity)
        case None =>
          LineItem(
            productId = addLineItem.productId,
            name = addLineItem.name,
            quantity = addLineItem.quantity)
      }
      val updatedCart = cart + (item.productId -> item)
      val updatedState = currentState.withItems(updatedCart.values.toSeq)
      effects.updateState(updatedState)
        .thenReply(Empty.defaultInstance)
    }
  // end::add-item[]

  // tag::removeItem[]
  override def removeItem(currentState: Cart, removeLineItem: shoppingcart.RemoveLineItem): ValueEntity.Effect[Empty] =
    if (!currentState.items.exists(_.productId == removeLineItem.productId)) {
      effects.error(s"Cannot remove item ${removeLineItem.productId} because it is not in the cart.")
    } else {
      val updatedState = currentState.withItems(currentState.items.filterNot(_.productId == removeLineItem.productId))
      effects.updateState(updatedState)
        .thenReply(Empty.defaultInstance)
    }
  // end::removeItem[]

  // tag::get-cart[]
  override def getCart(currentState: Cart, getShoppingCart: shoppingcart.GetShoppingCart): ValueEntity.Effect[shoppingcart.Cart] =
    effects.reply(toApi(currentState))

  private def toApi(cart: Cart): shoppingcart.Cart =
    shoppingcart.Cart(
      items = cart.items.map(toApi),
      creationTimestamp = cart.creationTimestamp)

  private def toApi(item: LineItem): shoppingcart.LineItem =
    shoppingcart.LineItem(
      productId = item.productId,
      name = item.name,
      quantity = item.quantity
    )
  // end::get-cart[]

  override def removeCart(currentState: Cart, removeShoppingCart: shoppingcart.RemoveShoppingCart): ValueEntity.Effect[Empty] = {
    commandContext().metadata.get("Role") match {
      case Some("Admin") => effects.deleteEntity().thenReply(Empty.defaultInstance)
      case any => effects.error("Only admin can remove the cart")
    }
  }

}

