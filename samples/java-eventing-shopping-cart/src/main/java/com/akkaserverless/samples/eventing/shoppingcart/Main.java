/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventing.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import shopping.cart.actions.TopicPublisher;
import shopping.cart.actions.ShoppingCartAnalytics;
import shopping.cart.actions.ToProductPopularity;
import shopping.cart.api.ShoppingCartApi;
import shopping.cart.model.ShoppingCart;
import shopping.cart.view.ShoppingCartViewModel;
import shopping.product.api.ProductApi;
import shopping.product.model.Product;

public final class Main {
  public static final void main(String[] args) throws Exception {
    new AkkaServerless()
        // event sourced shopping cart entity
        // receives commands from external world and persist events to event log
        .registerEventSourcedEntity(
            ShoppingCartEntity.class,
            ShoppingCartApi.getDescriptor().findServiceByName("ShoppingCartService"),
            ShoppingCart.getDescriptor())

        // consume shopping cart events directly from event log
        // and publish as is to 'shopping-cart-events' topic
        .registerAction(
            new TopicPublisherAction(),
            TopicPublisher.getDescriptor().findServiceByName("TopicPublisherService"))

        // consume shopping cart events published to 'shopping-cart-events' topic
        .registerAction(
            new ShoppingCartAnalyticsAction(),
            ShoppingCartAnalytics.getDescriptor().findServiceByName("ShoppingCartAnalyticsService"))

        // consume shopping cart events directly from event log
        // and send as commands to ProductPopularityEntity
        .registerAction(
            new ToProductPopularityAction(),
            ToProductPopularity.getDescriptor().findServiceByName("ToProductPopularityService"))

        // value entity tracking product popularity
        .registerValueEntity(
            ProductPopularityEntity.class,
            ProductApi.getDescriptor().findServiceByName("ProductPopularityService"),
            Product.getDescriptor())

        // view of the shopping carts
        .registerView(
            ShoppingCartView.class,
            shopping.cart.view.ShoppingCartViewModel.getDescriptor()
                .findServiceByName("ShoppingCartViewService"),
            "carts",
            ShoppingCart.getDescriptor(),
            ShoppingCartViewModel.getDescriptor())
        .start()
        .toCompletableFuture()
        .get();
  }
}
