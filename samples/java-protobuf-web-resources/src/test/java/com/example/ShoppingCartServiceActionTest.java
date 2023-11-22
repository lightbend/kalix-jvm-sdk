package com.example;

import akka.stream.javadsl.Source;
import com.example.ShoppingCartServiceAction;
import com.example.ShoppingCartServiceActionTestKit;
import com.example.WebResources;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ActionResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class ShoppingCartServiceActionTest {

  @Test
  @Disabled("to be implemented")
  public void exampleTest() {
    ShoppingCartServiceActionTestKit service = ShoppingCartServiceActionTestKit.of(ShoppingCartServiceAction::new);
    // // use the testkit to execute a command
    // SomeCommand command = SomeCommand.newBuilder()...build();
    // ActionResult<SomeResponse> result = service.someOperation(command);
    // // verify the reply
    // SomeReply reply = result.getReply();
    // assertEquals(expectedReply, reply);
  }

  @Test
  @Disabled("to be implemented")
  public void getCartTest() {
    ShoppingCartServiceActionTestKit testKit = ShoppingCartServiceActionTestKit.of(ShoppingCartServiceAction::new);
    // ActionResult<WebResources.ShoppingCart> result = testKit.getCart(Empty.newBuilder()...build());
  }

}
