package com.example.domain;

import io.grpc.Status;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.springsdk.annotations.Entity;
import org.springframework.web.bind.annotation.*;

// tag::order[]
@Entity(entityType = "order", entityKey = "id")
@RequestMapping("/order/{id}")
public class OrderEntity extends ValueEntity<Order> {

  private final String entityId;

  public OrderEntity(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public Order emptyState() {
    return new Order(entityId, false, false, "", 0);
  }

  @PostMapping("/place")
  public Effect<Order> placeOrder(@PathVariable String id, @RequestBody OrderRequest orderRequest) {
    var newOrder = new Order(id, false, true, orderRequest.item(), orderRequest.quantity());
    return effects()
            .updateState(newOrder)
            .thenReply(newOrder);
  }

  @PostMapping("/confirm")
  public Effect<String> confirm(@PathVariable String id) {
    if (currentState().placed()) { // <2>
      return effects()
          .updateState(currentState().confirm())
          .thenReply("Ok");
    } else {
      return effects().error(
        "No order found for '" + id + "'",
          Status.Code.NOT_FOUND); // <3>
    }
  }

  @PostMapping("/cancel")
  public Effect<String> cancel(@PathVariable String id) {
    if (!currentState().placed()) {
      return effects().error(
          "No order found for " + id,
          Status.Code.NOT_FOUND); // <4>
    } else if (currentState().confirmed()) {
      return effects().error(
          "Cannot cancel an already confirmed order",
          Status.Code.INVALID_ARGUMENT); // <5>
    } else {
      return effects().updateState(emptyState())
          .thenReply("Ok"); // <6>
    }
  }
  // end::order[]

  @GetMapping
  public Effect<OrderStatus> status(@PathVariable String id) {
    if (currentState().placed()) {
      var orderStatus = new OrderStatus(id, currentState().item(), currentState().quantity(), currentState().confirmed());
      return effects().reply(orderStatus);
    } else {
      return effects().error(
          "No order found for '" + id + "'",
          Status.Code.NOT_FOUND);
    }
  }
// tag::order[]
}
// end::order[]