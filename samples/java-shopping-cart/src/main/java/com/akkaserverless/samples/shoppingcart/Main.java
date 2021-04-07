/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.valueentity.shoppingcart.ShoppingCart;

public final class Main {
  public static AkkaServerless shoppingCart =
      new AkkaServerless()
          .registerValueEntity(
              ShoppingCartEntity.class,
              ShoppingCart.getDescriptor().findServiceByName("ShoppingCartService"),
              com.example.valueentity.shoppingcart.persistence.Domain.getDescriptor());

  public static final void main(String[] args) throws Exception {
    shoppingCart.start().toCompletableFuture().get();
  }
}
