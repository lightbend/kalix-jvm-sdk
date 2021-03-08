/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventing.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.eventing.shoppingcart.Shoppingcart;
import com.example.eventing.shoppingcart.analytics.ShoppingcartAnalytics;
import com.example.eventing.shoppingcart.publisher.ShoppingcartPublisher;

public final class Main {
  public static final void main(String[] args) throws Exception {
    new AkkaServerless()
        .registerEventSourcedEntity(
            ShoppingCartEntity.class,
            Shoppingcart.getDescriptor().findServiceByName("ShoppingCart"),
            com.example.eventing.shoppingcart.persistence.Domain.getDescriptor())
        .registerAction(
            new ShoppingCartAnalytics(),
            ShoppingcartAnalytics.getDescriptor().findServiceByName("ShoppingCartAnalytics"))
        .registerAction(
            new ShoppingCartPublisher(),
            ShoppingcartPublisher.getDescriptor().findServiceByName("ShoppingCartEventPublisher"))
        .start()
        .toCompletableFuture()
        .get();
  }
}
