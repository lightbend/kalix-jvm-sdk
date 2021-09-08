/*
 * Copyright (C) 2009-2021 Lightbend Inc. <http://www.lightbend.com>
 */
package com.example.shoppingcart.domain;

import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl;
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityEffectImpl$;
import com.akkaserverless.javasdk.testkit.EventSourcedResult;
import com.akkaserverless.javasdk.testkit.ValueEntityResult;
import com.akkaserverless.javasdk.testkit.impl.AkkaServerlessTestKitHelper;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCart;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Empty;

import java.util.Collections;

// FIXME to be generated
public class ShoppingCartTestKit {

  private ShoppingCartDomain.Cart state;
  private ShoppingCart entity;

  private AkkaServerlessTestKitHelper helper = new AkkaServerlessTestKitHelper<ShoppingCartDomain.Cart>();

  public ShoppingCartTestKit(ShoppingCart entity) {
      this.state = entity.emptyState();
      this.entity = entity;
    }

  public ShoppingCartTestKit(ShoppingCart entity, ShoppingCartDomain.Cart state) {
      this.state = state;
      this.entity = entity;
    }

  public ShoppingCartDomain.Cart getState() {
      return state;
    }


    private <Reply> Reply getReplyOfType(ValueEntity.Effect<Reply> effect, ShoppingCartDomain.Cart state) {
      return (Reply) helper.getReply(effect);
    }

    private <Reply> ValueEntityResult<Reply> interpretEffects(ValueEntity.Effect<Reply> effect) {
      helper.updatedStateFrom(effect).ifPresent(state ->
          this.state = (ShoppingCartDomain.Cart) state
      );
      Reply reply = this.<Reply>getReplyOfType(effect, this.state);
      return new ValueEntityResult(reply);
    }

    public ValueEntityResult<Empty> addItem(ShoppingCartApi.AddLineItem addLineItem) {
      ValueEntity.Effect<Empty> effect = entity.addItem(state, addLineItem);
      return interpretEffects(effect);
    }

    // and ... generated for each command
}
