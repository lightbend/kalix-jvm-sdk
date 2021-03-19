/*
 * Copyright 2019 Lightbend Inc.
 */

package docs.user.gettingstarted;

// tag::shopping-cart-main[]
import com.akkaserverless.javasdk.AkkaServerless;
import com.example.Shoppingcart;

public class ShoppingCartMain {

  public static void main(String... args) {
    new AkkaServerless()
        .registerEventSourcedEntity(
            ShoppingCartEntity.class,
            Shoppingcart.getDescriptor().findServiceByName("ShoppingCartService"))
        .start();
  }
}
// end::shopping-cart-main[]

class ShoppingCartEntity {}
