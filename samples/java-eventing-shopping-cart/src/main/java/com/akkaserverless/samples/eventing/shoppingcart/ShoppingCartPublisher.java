/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventing.shoppingcart;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.CommandHandler;
import com.example.eventing.shoppingcart.persistence.Domain;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Action
public class ShoppingCartPublisher {
  private static final Logger LOG = LoggerFactory.getLogger(ShoppingCartPublisher.class);

  @CommandHandler
  public Domain.ItemAdded publishAdded(Domain.ItemAdded in) {
    LOG.info("Publishing: {}", in);
    return in;
  }

  @CommandHandler
  public Empty processIgnore(Any event) {
    LOG.info("Publishing: ignoring {}", event);
    return Empty.getDefaultInstance();
  }
}
