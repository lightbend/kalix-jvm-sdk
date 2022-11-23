package org.example.view

import kalix.scalasdk.view.View

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

abstract class AbstractCustomerOrdersView extends View {

  override def emptyState: Any = {
    updateContext().viewTable match {
      case "customers" => emptyCustomerState
      case "products" => emptyProductState
      case _ => null
    }
  }

  def emptyCustomerState: CustomerState

  def emptyProductState: ProductState

  def updateCustomerCreated(
    state: CustomerState, customerCreated: CustomerCreated): View.UpdateEffect[CustomerState]

  def updateCustomerNameChanged(
    state: CustomerState, customerNameChanged: CustomerNameChanged): View.UpdateEffect[CustomerState]

  def updateProduct(
    state: ProductState, productCreated: ProductCreated): View.UpdateEffect[ProductState]
}
