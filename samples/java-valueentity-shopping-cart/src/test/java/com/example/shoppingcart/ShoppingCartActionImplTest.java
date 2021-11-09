package com.example.shoppingcart;

import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.testkit.ActionResult;
import com.example.shoppingcart.ShoppingCartActionImpl;
import com.example.shoppingcart.ShoppingCartActionImplTestKit;
import com.example.shoppingcart.ShoppingCartController;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShoppingCartActionImplTest {

  /* Cannot be tested with unit testkit as it involves other components,
     needs an integration test for testing.
  @Test
  public void initializeCartTest() {
    ShoppingCartActionImplTestKit testKit = ShoppingCartActionImplTestKit.of(ShoppingCartActionImpl::new);
    // ActionResult<ShoppingCartController.NewCartCreated> result = testKit.initializeCart(ShoppingCartController.NewCart.newBuilder()...build());
  }
   */

}
