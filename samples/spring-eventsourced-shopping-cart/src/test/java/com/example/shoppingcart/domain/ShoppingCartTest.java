package com.example.shoppingcart.domain;

import akka.actor.Address;
import com.example.shoppingcart.ShoppingCartService;
import kalix.javasdk.testkit.EventSourcedResult;
import kalix.springsdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;

import javax.sound.sampled.Line;

import java.util.List;

import static com.example.shoppingcart.domain.ShoppingCartEvent.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShoppingCartTest {

  private final ShoppingCart.LineItem akkaTshirt = new ShoppingCart.LineItem("akka-tshirt", "Akka Tshirt", 10);


  @Test
  public void testAddLineItem() {

    EventSourcedTestKit<ShoppingCart, ShoppingCartService> testKit = EventSourcedTestKit.of(ShoppingCartService::new);
    {
      EventSourcedResult<String> result = testKit.call(e -> e.addItem(akkaTshirt));
      assertEquals("OK", result.getReply());
      assertEquals(10, result.getNextEventOfType(ItemAdded.class).item().quantity());
    }

    // actually we want more akka tshirts
    {
      EventSourcedResult<String> result = testKit.call(e -> e.addItem(akkaTshirt.withQuantity(5)));
      assertEquals("OK", result.getReply());
      assertEquals(5, result.getNextEventOfType(ItemAdded.class).item().quantity());
    }

    {
      EventSourcedResult<ShoppingCart> result = testKit.call(ShoppingCartService::getCart);
      assertEquals(new ShoppingCart("testkit-entity-id", List.of(akkaTshirt.withQuantity(15))), result.getReply());
    }

  }

}
