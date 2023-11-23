package com.example;

import com.example.actions.Order;
import com.example.actions.OrderApi;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.concurrent.ExecutionException;

import static java.util.concurrent.TimeUnit.SECONDS;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

// Example of an integration test calling our service via the Kalix Runtime
// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class OrderIntegrationTest {

  /**
   * The test kit starts both the service container and the Kalix Runtime.
   */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
    new KalixTestKitExtension(Main.createKalix());

  /**
   * Use the generated gRPC client to call the service through the Kalix Runtime.
   */
  private final OrderService orderClient;
  private final Order actionClient;

  public OrderIntegrationTest() {
    orderClient = testKit.getGrpcClient(OrderService.class);
    actionClient = testKit.getGrpcClient(Order.class);
  }

  @Test
  @Disabled("to be implemented")
  public void placeOrderOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.placeOrder(OrderServiceApi.OrderRequest.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }

  @Test
  @Disabled("to be implemented")
  public void confirmOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.confirm(OrderServiceApi.ConfirmRequest.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }

  @Test
  public void expireOnNonExistingEntity() throws Exception {
    // the expire endpoint is made to be used internally by timers
    // thus, in case the order does not exist, it should return successfully so the timer is not rescheduled
    try {
      actionClient.expire(OrderApi.OrderNumber.newBuilder().setNumber("unknown-number").build())
          .toCompletableFuture().get(5, SECONDS);
    } catch (Exception e) {
      Assertions.fail("Should not reach this");
    }
  }

  @Test
  @Disabled("to be implemented")
  public void getOrderStatusOnNonExistingEntity() throws Exception {
    // TODO: set fields in command, and provide assertions to match replies
    // client.getOrderStatus(OrderServiceApi.OrderStatusRequest.newBuilder().build())
    //         .toCompletableFuture().get(5, SECONDS);
  }
}
