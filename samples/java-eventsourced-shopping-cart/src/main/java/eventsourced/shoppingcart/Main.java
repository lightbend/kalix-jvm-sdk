/*
 * Copyright 2021 Lightbend Inc.
 */

package eventsourced.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.shoppingcart.ShoppingCartApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
  public static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static final AkkaServerless SERVICE =
      new AkkaServerless()
          .registerEventSourcedEntity(
              ShoppingCartEntity.class,
              ShoppingCartApi.getDescriptor().findServiceByName("ShoppingCartService"),
              com.example.shoppingcart.domain.ShoppingCartDomain.getDescriptor());

  public static final void main(String[] args) throws Exception {
    LOG.info("started");
    SERVICE.start().toCompletableFuture().get();
  }
}
