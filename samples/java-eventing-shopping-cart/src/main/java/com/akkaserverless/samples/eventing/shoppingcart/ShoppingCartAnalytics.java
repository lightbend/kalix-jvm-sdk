/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventing.shoppingcart;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.CommandHandler;
import com.example.eventing.shoppingcart.persistence.Domain;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;

@Action
public class ShoppingCartAnalytics {

  @CommandHandler
  public Empty processAddedViaTopic(Domain.ItemAdded event) {
    System.out.println("Analytics: item added (via topic) : " + event);
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty processRemoved(Domain.ItemRemoved event) {
    System.out.println("Analytics: item removed: " + event);
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty processIgnore(Any event) {
    System.out.println("Analytics: ignoring: " + event);
    return Empty.getDefaultInstance();
  }
}
