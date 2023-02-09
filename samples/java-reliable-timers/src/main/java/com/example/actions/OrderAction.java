package com.example.actions;

import akka.Done;
import com.example.OrderServiceApi;
import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import kalix.javasdk.action.ActionCreationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Action Service described in your com/example/actions/order_action.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// tag::timers[]
public class OrderAction extends AbstractOrderAction {
// end::timers[]

  private Logger logger = LoggerFactory.getLogger(getClass());

  public OrderAction(ActionCreationContext creationContext) {
  }

  // tag::place-order[]
  private String timerName(OrderApi.OrderNumber orderNum) {
    return "order-expiration-timer-" + orderNum.getNumber();
  }

  @Override
  public Effect<OrderApi.OrderNumber> placeOrder(OrderApi.OrderRequest orderRequest) {

    OrderApi.OrderNumber orderNumber = // <1>
        OrderApi.OrderNumber
            .newBuilder()
            .setNumber(UUID.randomUUID().toString())
            .build();

    CompletionStage<Done> timerRegistration = // <2>
        timers().startSingleTimer(
            timerName(orderNumber), // <3>
            Duration.ofMinutes(5), // <4>
            components().orderAction().expire(orderNumber) // <5>
        );

    OrderServiceApi.OrderRequest request = // <6>
        OrderServiceApi.OrderRequest.newBuilder()
            .setOrderNumber(orderNumber.getNumber())
            .setItem(orderRequest.getItem())
            .setQuantity(orderRequest.getQuantity())
            .build();

    // end::place-order[]
    logger.info(
        "Placing order for item {} (quantity {}). Order number '{}'",
        orderRequest.getItem(),
        orderRequest.getQuantity(),
        orderNumber.getNumber());
    // tag::place-order[]

    return effects().asyncReply( // <7>
        timerRegistration
            .thenCompose(done -> components().order().placeOrder(request).execute())
            .thenApply(empty -> orderNumber)
    );
  }
  // end::place-order[]

  // tag::expire-order[]
  @Override
  public Effect<Empty> expire(OrderApi.OrderNumber orderNumber) {
    logger.info("Expiring order '{}'", orderNumber.getNumber());

    Predicate<StatusRuntimeException> validateErrorCodes = exception -> {
      Status.Code code = exception.getStatus().getCode();
      return code == Status.Code.NOT_FOUND || code == Status.Code.INVALID_ARGUMENT;
    };

    OrderServiceApi.CancelRequest cancelRequest =
        OrderServiceApi.CancelRequest.newBuilder()
            .setOrderNumber(orderNumber.getNumber())
            .build();

    CompletionStage<Empty> reply =
        components().order()
            .cancel(cancelRequest)
            .execute() // <1>
            .thenApply(cancelled -> Empty.getDefaultInstance()) // <2>
            .exceptionally(e -> { // <3>
                  if (e.getCause() instanceof StatusRuntimeException &&
                      validateErrorCodes.test((StatusRuntimeException) e.getCause())) {
                    // if NotFound or InvalidArgument, we don't need to re-try, and we can move on
                    // other kind of failures are not recovered and will trigger a re-try
                    return Empty.getDefaultInstance();
                  } else {
                    throw new StatusRuntimeException(Status.fromThrowable(e));
                  }
                }
            );
    return effects().asyncReply(reply);
  }
  // end::expire-order[]

  // tag::confirm-cancel-order[]
  @Override
  public Effect<Empty> confirm(OrderApi.OrderNumber orderNumber) {
    logger.info("Confirming order '{}'", orderNumber.getNumber());
    OrderServiceApi.ConfirmRequest request =
        OrderServiceApi.ConfirmRequest.newBuilder()
            .setOrderNumber(orderNumber.getNumber())
            .build();

    CompletionStage<Empty> reply =
        components().order() // <1>
            .confirm(request)
            .execute()
            .thenCompose(req -> timers().cancel(timerName(orderNumber))) // <2>
            .thenApply(done -> Empty.getDefaultInstance());

    return effects().asyncReply(reply);
  }

  @Override
  public Effect<Empty> cancel(OrderApi.OrderNumber orderNumber) {
    logger.info("Cancelling order '{}'", orderNumber.getNumber());
    OrderServiceApi.CancelRequest request =
        OrderServiceApi.CancelRequest.newBuilder()
            .setOrderNumber(orderNumber.getNumber())
            .build();

    CompletionStage<Empty> reply =
        components().order()
            .cancel(request)
            .execute()
            .thenCompose(req -> timers().cancel(timerName(orderNumber)))
            .thenApply(done -> Empty.getDefaultInstance());

    return effects().asyncReply(reply);
  }
  // end::confirm-cancel-order[]

  @Override
  public Effect<OrderServiceApi.OrderStatus> getOrderStatus(OrderApi.OrderNumber orderNumber) {
    throw new RuntimeException("The command handler for `GetOrderStatus` is not implemented, yet");
  }

// tag::timers[]
}
// end::timers[]
