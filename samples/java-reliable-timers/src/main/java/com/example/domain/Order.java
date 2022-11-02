package com.example.domain;

import com.example.OrderServiceApi;
import com.example.actions.OrderApi;
import com.google.protobuf.Empty;
import io.grpc.Status;
import kalix.javasdk.valueentity.ValueEntityContext;

import java.util.UUID;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Value Entity Service described in your com/example/order_service_api.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::order[]
public class Order extends AbstractOrder {
  @SuppressWarnings("unused")
  private final String entityId;

  public Order(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public OrderDomain.OrderState emptyState() {
    return OrderDomain.OrderState.getDefaultInstance();
  }


  @Override
  public Effect<Empty> placeOrder(OrderDomain.OrderState currentState, OrderServiceApi.OrderRequest orderRequest) { // <1>
    OrderDomain.OrderState orderState =
        OrderDomain.OrderState.newBuilder()
            .setOrderNumber(orderRequest.getOrderNumber())
            .setItem(orderRequest.getItem())
            .setQuantity(orderRequest.getQuantity())
            .setPlaced(true) // <2>
            .build();

    return effects().updateState(orderState).thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> confirm(OrderDomain.OrderState currentState, OrderServiceApi.ConfirmRequest confirmRequest) {
    if (currentState.getPlaced()) { // <3>
      return effects()
          .updateState(currentState.toBuilder().setConfirmed(true).build())
          .thenReply(Empty.getDefaultInstance());
    } else {
      return effects().error(
        "No order found for '" + confirmRequest.getOrderNumber() + "'",
          Status.Code.NOT_FOUND); // <4>
    }
  }

  @Override
  public Effect<Empty> cancel(OrderDomain.OrderState currentState, OrderServiceApi.CancelRequest cancelRequest) {
    if (!currentState.getPlaced()) {
      return effects().error(
          "No order found for " + cancelRequest.getOrderNumber(),
          Status.Code.NOT_FOUND); // <5>
    } else if (currentState.getConfirmed()) {
      return effects().error(
          "Can not cancel an already confirmed order",
          Status.Code.INVALID_ARGUMENT); // <6>
    } else {
      return effects().updateState(emptyState())
          .thenReply(Empty.getDefaultInstance()); // <7>
    }
  }
  // end::order[]
  @Override
  public Effect<OrderServiceApi.OrderStatus> getOrderStatus(OrderDomain.OrderState currentState, OrderServiceApi.OrderStatusRequest orderStatusRequest) {

    if (currentState.getPlaced()) {

      OrderServiceApi.OrderStatus status =
          OrderServiceApi.OrderStatus.newBuilder()
              .setConfirmed(currentState.getConfirmed())
              .setItem(currentState.getItem())
              .setQuantity(currentState.getQuantity())
              .build();

      return effects().reply(status);
    } else {
      return effects().error(
          "No order found for '" + orderStatusRequest.getOrderNumber() + "'",
          Status.Code.NOT_FOUND);
    }
  }
// tag::order[]
}
// end::order[]