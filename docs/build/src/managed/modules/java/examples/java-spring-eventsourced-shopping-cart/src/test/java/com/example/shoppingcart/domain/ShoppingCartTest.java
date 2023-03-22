package com.example.shoppingcart.domain;

import com.example.shoppingcart.ShoppingCartEntity;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.example.shoppingcart.domain.ShoppingCartEvent.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShoppingCartTest {

  private final ShoppingCart.LineItem akkaTshirt = new ShoppingCart.LineItem("kalix-tshirt", "Akka Tshirt", 10);


  @Test
  public void testAddLineItem() {

    var testKit = EventSourcedTestKit.of(ShoppingCartEntity::new); // <1>

    {
      var result = testKit.call(e -> e.addItem(akkaTshirt)); // <2>
      assertEquals("OK", result.getReply()); // <3>

      var itemAdded = result.getNextEventOfType(ItemAdded.class);
      assertEquals(10, itemAdded.item().quantity()); // <4>
    }

    // actually we want more akka tshirts
    {
      var result = testKit.call(e -> e.addItem( akkaTshirt.withQuantity(5))); // <5>
      assertEquals("OK", result.getReply());

      var itemAdded = result.getNextEventOfType(ItemAdded.class);
      assertEquals(5, itemAdded.item().quantity());
    }

    {
      assertEquals(testKit.getAllEvents().size(), 2); // <6>
      var result = testKit.call(ShoppingCartEntity::getCart); // <7>
      assertEquals(
        new ShoppingCart("testkit-entity-id", List.of(akkaTshirt.withQuantity(15)), false),
        result.getReply());
    }

  }

}
