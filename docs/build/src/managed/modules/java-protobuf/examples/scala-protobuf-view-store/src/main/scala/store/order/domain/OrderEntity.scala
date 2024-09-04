package store.order.domain

import java.time.Instant

import com.google.protobuf.empty.Empty
import com.google.protobuf.timestamp.Timestamp
import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityContext
import store.order.api

class OrderEntity(context: ValueEntityContext) extends AbstractOrderEntity {

  override def emptyState: OrderState = OrderState.defaultInstance

  override def create(currentState: OrderState, order: api.Order): ValueEntity.Effect[Empty] = {
    val now = Some(Timestamp(Instant.now()))
    val orderState = OrderState(
      orderId = order.orderId,
      productId = order.productId,
      customerId = order.customerId,
      quantity = order.quantity,
      created = now)
    effects.updateState(orderState).thenReply(Empty.defaultInstance)
  }

  override def get(currentState: OrderState, getOrder: api.GetOrder): ValueEntity.Effect[api.Order] = {
    val order = api.Order(
      orderId = currentState.orderId,
      productId = currentState.productId,
      customerId = currentState.customerId,
      quantity = currentState.quantity)
    effects.reply(order)
  }
}
