package com.example.actions;

import akka.Done;
import com.example.domain.OrderEntity;
import com.example.domain.OrderRequest;
import com.example.domain.Order;
import kalix.javasdk.DeferredCallResponseException;
import kalix.javasdk.StatusCode.ErrorCode;
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

// tag::timers[]
@RequestMapping("/orders")
public class OrderAction extends Action {
// end::timers[]

  private Logger logger = LoggerFactory.getLogger(getClass());

  private ComponentClient componentClient;

  private ActionCreationContext ctx;


  public OrderAction(ActionCreationContext creationContext, ComponentClient componentClient) {
    this.ctx = creationContext;
    this.componentClient = componentClient;
  }

  // tag::place-order[]
  private String timerName(String orderId) {
    return "order-expiration-timer-" + orderId;
  }

  @PostMapping("/place")
  public Effect<Order> placeOrder(@RequestBody OrderRequest orderRequest) {

    var orderId = UUID.randomUUID().toString(); // <1>

    CompletionStage<Done> timerRegistration = // <2>
        timers().startSingleTimer(
          timerName(orderId), // <3>
          Duration.ofSeconds(10), // <4>
          componentClient.forAction().call(OrderAction::expire).params(orderId) // <5>
        );

    // end::place-order[]
    logger.info(
        "Placing order for item {} (quantity {}). Order number '{}'",
        orderRequest.item(),
        orderRequest.quantity(),
        orderId);
    // tag::place-order[]

    var request = componentClient.forValueEntity(orderId).call(OrderEntity::placeOrder).params(orderRequest); // <6>
    return effects().asyncReply( // <7>
        timerRegistration
            .thenCompose(done -> request.execute())
            .thenApply(order -> order)
    );
  }
  // end::place-order[]

  // tag::expire-order[]
  // ...

  @PostMapping("/expire/{orderId}")
  public Effect<String> expire(@PathVariable String orderId) {
    logger.info("Expiring order '{}'", orderId);
    var cancelRequest = componentClient.forValueEntity(orderId).call(OrderEntity::cancel);

    CompletionStage<String> reply =
        cancelRequest
            .execute() // <1>
            .thenApply(cancelled -> "Ok") // <2>
            .exceptionally(e -> { // <3>
                  if (e.getCause() instanceof DeferredCallResponseException dcre &&
                      Set.of(ErrorCode.NOT_FOUND, ErrorCode.BAD_REQUEST).contains(dcre.errorCode())) {
                    // if NotFound or InvalidArgument, we don't need to re-try, and we can move on
                    // other kind of failures are not recovered and will trigger a re-try
                    return "Ok";
                  } else {
                    throw new RuntimeException(e);
                  }
                }
            );
    return effects().asyncReply(reply);
  }
  // end::expire-order[]

  // tag::confirm-cancel-order[]
  // ...

  @PostMapping("/confirm/{orderId}")
  public Effect<String> confirm(@PathVariable String orderId) {
    logger.info("Confirming order '{}'", orderId);

    CompletionStage<String> reply =
      componentClient.forValueEntity(orderId).call(OrderEntity::confirm) // <1>
        .execute()
            .thenCompose(req -> timers().cancel(timerName(orderId))) // <2>
            .thenApply(done -> "Ok");

    return effects().asyncReply(reply);
  }

  @PostMapping("/cancel/{orderId}")
  public Effect<String> cancel(@PathVariable String orderId) {
    logger.info("Cancelling order '{}'", orderId);

    CompletionStage<String> reply =
      componentClient.forValueEntity(orderId).call(OrderEntity::cancel)
            .execute()
            .thenCompose(req -> timers().cancel(timerName(orderId)))
            .thenApply(done -> "Ok");

    return effects().asyncReply(reply);
  }
  // end::confirm-cancel-order[]

// tag::timers[]
}
// end::timers[]
