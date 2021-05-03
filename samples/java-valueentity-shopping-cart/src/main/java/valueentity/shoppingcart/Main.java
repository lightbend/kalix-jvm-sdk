/*
 * Copyright 2021 Lightbend Inc.
 */

// tag::main-class[]
package valueentity.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.valueentity.shoppingcart.ShoppingCartApi;
import com.example.valueentity.shoppingcart.domain.ShoppingCartDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static final AkkaServerless SERVICE =
      new AkkaServerless()
          .registerValueEntity(
              ShoppingCartEntity.class,
              ShoppingCartApi.getDescriptor().findServiceByName("ShoppingCartService"),
              ShoppingCartDomain.getDescriptor());

  public static final void main(String[] args) throws Exception {
    LOG.info("started");
    SERVICE.start().toCompletableFuture().get();
  }
}
// end::main-class[]
