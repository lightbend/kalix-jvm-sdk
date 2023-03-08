package store.order.domain;

import java.time.Instant;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.Timestamps;
import kalix.javasdk.valueentity.ValueEntityContext;
import store.order.api.OrderApi;

public class OrderEntity extends AbstractOrderEntity {

  public OrderEntity(ValueEntityContext context) {}

  @Override
  public OrderDomain.OrderState emptyState() {
    return OrderDomain.OrderState.getDefaultInstance();
  }

  @Override
  public Effect<Empty> create(OrderDomain.OrderState currentState, OrderApi.Order order) {
    Timestamp now = Timestamps.fromMillis(Instant.now().toEpochMilli());
    OrderDomain.OrderState orderState =
        OrderDomain.OrderState.newBuilder()
            .setOrderId(order.getOrderId())
            .setProductId(order.getProductId())
            .setCustomerId(order.getCustomerId())
            .setQuantity(order.getQuantity())
            .setCreated(now)
            .build();
    return effects().updateState(orderState).thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<OrderApi.Order> get(
      OrderDomain.OrderState currentState, OrderApi.GetOrder getOrder) {
    OrderApi.Order order =
        OrderApi.Order.newBuilder()
            .setOrderId(currentState.getOrderId())
            .setProductId(currentState.getProductId())
            .setCustomerId(currentState.getCustomerId())
            .setQuantity(currentState.getQuantity())
            .build();
    return effects().reply(order);
  }
}
