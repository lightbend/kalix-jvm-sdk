package com.example.actions;

import akka.stream.javadsl.Source;
import com.example.OrderServiceApi;
import com.example.actions.OrderAction;
import com.example.actions.OrderActionTestKit;
import com.example.actions.OrderApi;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ActionResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class OrderActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    OrderActionTestKit service = OrderActionTestKit.of(OrderAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void placeOrderTest() {
    OrderActionTestKit testKit = OrderActionTestKit.of(OrderAction::new);
    // ActionResult<OrderApi.OrderNumber> result = testKit.placeOrder(OrderApi.OrderRequest.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void confirmTest() {
    OrderActionTestKit testKit = OrderActionTestKit.of(OrderAction::new);
    // ActionResult<Empty> result = testKit.confirm(OrderApi.OrderNumber.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void cancelTest() {
    OrderActionTestKit testKit = OrderActionTestKit.of(OrderAction::new);
    // ActionResult<Empty> result = testKit.cancel(OrderApi.OrderNumber.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void expireTest() {
    OrderActionTestKit testKit = OrderActionTestKit.of(OrderAction::new);
    // ActionResult<Empty> result = testKit.expire(OrderApi.OrderNumber.newBuilder()...build());
  }

  @Test
  @Disabled("to be implemented")
  public void getOrderStatusTest() {
    OrderActionTestKit testKit = OrderActionTestKit.of(OrderAction::new);
    // ActionResult<OrderServiceApi.OrderStatus> result = testKit.getOrderStatus(OrderApi.OrderNumber.newBuilder()...build());
  }

}
