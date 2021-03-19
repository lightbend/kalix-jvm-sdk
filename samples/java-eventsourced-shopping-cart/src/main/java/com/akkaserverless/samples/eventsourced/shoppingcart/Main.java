/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventsourced.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.shoppingcart.ShoppingCart;

public final class Main {
  public static final void main(String[] args) throws Exception {
    new AkkaServerless()
        .registerEventSourcedEntity(
            ShoppingCartEntity.class,
            ShoppingCart.getDescriptor().findServiceByName("ShoppingCartService"),
            com.example.shoppingcart.persistence.Domain.getDescriptor())
        .start()
        .toCompletableFuture()
        .get();
  }
}
