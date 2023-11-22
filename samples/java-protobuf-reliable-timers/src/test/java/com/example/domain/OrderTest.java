package com.example.domain;

import com.example.OrderServiceApi;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.valueentity.ValueEntity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class OrderTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    OrderTestKit service = OrderTestKit.of(Order::new);
    // // use the testkit to execute a command
    // // of events emitted, or a final updated state:
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ValueEntityResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
    // // verify the final state after the command
    // assertEquals(expectedState, service.getState());
  }

  @Test
  @Disabled("to be implemented")
  public void placeOrderTest() {
    OrderTestKit service = OrderTestKit.of(Order::new);
    // OrderRequest command = OrderRequest.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.placeOrder(command);
  }


  @Test
  @Disabled("to be implemented")
  public void confirmTest() {
    OrderTestKit service = OrderTestKit.of(Order::new);
    // ConfirmRequest command = ConfirmRequest.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.confirm(command);
  }


  @Test
  @Disabled("to be implemented")
  public void cancelTest() {
    OrderTestKit service = OrderTestKit.of(Order::new);
    // CancelRequest command = CancelRequest.newBuilder()...build();
    // ValueEntityResult<Empty> result = service.cancel(command);
  }


  @Test
  @Disabled("to be implemented")
  public void getOrderStatusTest() {
    OrderTestKit service = OrderTestKit.of(Order::new);
    // OrderStatusRequest command = OrderStatusRequest.newBuilder()...build();
    // ValueEntityResult<OrderStatus> result = service.getOrderStatus(command);
  }

}
