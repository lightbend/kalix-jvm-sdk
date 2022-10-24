package com.example.shoppingcart.domain;

import com.example.shoppingcart.ShoppingCartService;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.springsdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.shoppingcart.domain.ShoppingCartEvent.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShoppingCartTest {

  private final ShoppingCart.LineItem akkaTshirt = new ShoppingCart.LineItem("akka-tshirt", "Akka Tshirt", 10);


  @Test
  public void testAddLineItem() {

    EventSourcedTestKit<ShoppingCart, ShoppingCartService> testKit = EventSourcedTestKit.of(ShoppingCartService::new); // <1>
    {
      EventSourcedResult<String> result = testKit.call(e -> e.addItem(akkaTshirt)); // <2>
      assertEquals("OK", result.getReply()); // <3>
      assertEquals(10, result.getNextEventOfType(ItemAdded.class).item().quantity()); // <4>
    }

    // actually we want more akka tshirts
    {
      EventSourcedResult<String> result = testKit.call(e -> e.addItem( akkaTshirt.withQuantity(5))); // <5>
      assertEquals("OK", result.getReply());
      assertEquals(5, result.getNextEventOfType(ItemAdded.class).item().quantity());
    }

    {
      assertEquals(testKit.getAllEvents().size(), 2); // <6>
      EventSourcedResult<ShoppingCart> result = testKit.call(ShoppingCartService::getCart); // <7>
      assertEquals(new ShoppingCart("testkit-entity-id", List.of(akkaTshirt.withQuantity(15))), result.getReply());
    }

  }

}
