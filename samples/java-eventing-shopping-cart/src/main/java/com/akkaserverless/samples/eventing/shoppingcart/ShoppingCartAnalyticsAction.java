/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventing.shoppingcart;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.CommandHandler;
import com.google.protobuf.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.model.ShoppingCart;

/**
 * This action illustrates the consumption from a topic (shopping-cart-events) Incoming messages are
 * sent to log with no further processing.
 */
@Action
public class ShoppingCartAnalyticsAction {

  private static final Logger LOG = LoggerFactory.getLogger(ShoppingCartAnalyticsAction.class);

  @CommandHandler
  public Empty processAdded(ShoppingCart.ItemAdded event) {
    LOG.info("Analytics: item added '{}'", event);
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty processRemoved(ShoppingCart.ItemRemoved event) {
    LOG.info("Analytics: item removed '{}'", event);
    return Empty.getDefaultInstance();
  }
}
