/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.valueentity.shoppingcart.Shoppingcart;

public final class Main {
  public static final void main(String[] args) throws Exception {
    new AkkaServerless()
        .registerValueEntity(
            ShoppingCartEntity.class,
            Shoppingcart.getDescriptor().findServiceByName("ShoppingCart"),
            com.example.valueentity.shoppingcart.persistence.Domain.getDescriptor())
        .start()
        .toCompletableFuture()
        .get();
  }
}
