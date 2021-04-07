/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventsourced.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.shoppingcart.ShoppingCart;

public final class Main {
  public static AkkaServerless shoppingCart =
      new AkkaServerless()
          .registerEventSourcedEntity(
              ShoppingCartEntity.class,
              ShoppingCart.getDescriptor().findServiceByName("ShoppingCartService"),
              com.example.shoppingcart.persistence.Domain.getDescriptor());

  public static final void main(String[] args) throws Exception {
    shoppingCart.start().toCompletableFuture().get();
  }
}
