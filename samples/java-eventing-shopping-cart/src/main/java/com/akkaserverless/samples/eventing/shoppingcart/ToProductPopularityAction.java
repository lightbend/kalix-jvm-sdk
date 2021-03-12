/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventing.shoppingcart;

import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionContext;
import com.akkaserverless.javasdk.action.ActionReply;
import com.akkaserverless.javasdk.action.CommandHandler;
import com.google.protobuf.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.model.ShoppingCart;
import shopping.product.api.ProductApi;

@Action
public class ToProductPopularityAction {
  private static final Logger LOG = LoggerFactory.getLogger(ToProductPopularityAction.class);

  private final String serviceName = "shopping.product.api.ProductPopularityService";

  @CommandHandler
  public ActionReply<Empty> forwardAdded(ShoppingCart.ItemAdded in, ActionContext ctx) {

    ProductApi.IncreasePopularity increase =
        ProductApi.IncreasePopularity.newBuilder()
            .setProductId(in.getItem().getProductId())
            .setQuantity(in.getItem().getQuantity())
            .build();

    LOG.info("Received: '{}', publishing: {}", in, increase);
    ServiceCallRef<ProductApi.IncreasePopularity> call =
        ctx.serviceCallFactory()
            .lookup(serviceName, "Increase", ProductApi.IncreasePopularity.class);
    return ActionReply.forward(call.createCall(increase));
  }

  @CommandHandler
  public ActionReply<Empty> forwardRemoved(ShoppingCart.ItemRemoved in, ActionContext ctx) {

    ProductApi.DecreasePopularity decrease =
        ProductApi.DecreasePopularity.newBuilder()
            .setProductId(in.getProductId())
            .setQuantity(in.getQuantity())
            .build();

    LOG.info("Received: '{}', publishing: {}", in, decrease);
    ServiceCallRef<ProductApi.DecreasePopularity> call =
        ctx.serviceCallFactory()
            .lookup(serviceName, "Decrease", ProductApi.DecreasePopularity.class);
    return ActionReply.forward(call.createCall(decrease));
  }
}
