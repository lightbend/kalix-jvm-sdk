/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventing.shoppingcart;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.model.ShoppingCart;

@Action
public class TopicPublisherAction {
  private static final Logger LOG = LoggerFactory.getLogger(TopicPublisherAction.class);

  @CommandHandler
  public ShoppingCart.ItemAdded publishAdded(ShoppingCart.ItemAdded in) {
    LOG.info("Publishing: '{}' to topic", in);
    return in;
  }

  @CommandHandler
  public ShoppingCart.ItemRemoved publishRemoved(ShoppingCart.ItemRemoved in) {
    LOG.info("Publishing: '{}' to topic", in);
    return in;
  }
}
