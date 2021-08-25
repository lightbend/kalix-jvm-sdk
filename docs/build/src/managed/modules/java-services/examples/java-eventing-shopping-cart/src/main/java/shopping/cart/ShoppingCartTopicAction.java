/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package shopping.cart;

import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionContext;
import com.akkaserverless.javasdk.action.Handler;
import com.google.protobuf.Empty;
import shopping.cart.api.ShoppingCartApi;
import shopping.cart.api.ShoppingCartTopic;

/** This action illustrates the consumption from a topic (shopping-cart-operations). */
@Action
public class ShoppingCartTopicAction {

  private final String forwardTo = "shopping.cart.api.ShoppingCartService";

  /** Akka Serverless expects some CloudEvent metadata to determine the target protobuf type. */
  @Handler
  public Reply<Empty> protobufFromTopic(
      ShoppingCartTopic.TopicOperation message, ActionContext ctx) {
    if ("add".equals(message.getOperation())) {
      ShoppingCartApi.AddLineItem increase =
          ShoppingCartApi.AddLineItem.newBuilder()
              .setCartId(message.getCartId())
              .setProductId(message.getProductId())
              .setName(message.getName())
              .setQuantity(message.getQuantity())
              .build();

      ServiceCallRef<ShoppingCartApi.AddLineItem> call =
          ctx.serviceCallFactory().lookup(forwardTo, "AddItem", ShoppingCartApi.AddLineItem.class);
      return Reply.forward(call.createCall(increase));
    } else {
      return Reply.failure("The operation [" + message.getOperation() + "] is not implemented.");
    }
  }

  /**
   * Note that the protobuf rpc is declared with `Any`, but this method accepts a class annotated
   * with `@com.akkaserverless.javasdk.Jsonable`.
   */
  @Handler
  public Reply<Empty> jsonFromTopic(TopicMessage message, ActionContext ctx) {
    if ("add".equals(message.getOperation())) {
      ShoppingCartApi.AddLineItem add =
          ShoppingCartApi.AddLineItem.newBuilder()
              .setCartId(message.getCartId())
              .setProductId(message.getProductId())
              .setName(message.getName())
              .setQuantity(message.getQuantity())
              .build();
      ServiceCallRef<ShoppingCartApi.AddLineItem> addItemCall =
          ctx.serviceCallFactory().lookup(forwardTo, "AddItem", ShoppingCartApi.AddLineItem.class);
      return Reply.forward(addItemCall.createCall(add));
    } else if ("remove".equals(message.getOperation())) {
      ShoppingCartApi.RemoveLineItem remove =
          ShoppingCartApi.RemoveLineItem.newBuilder()
              .setCartId(message.getCartId())
              .setProductId(message.getProductId())
              .setQuantity(message.getQuantity())
              .build();
      ServiceCallRef<ShoppingCartApi.RemoveLineItem> removeItemCall =
          ctx.serviceCallFactory()
              .lookup(forwardTo, "RemoveItem", ShoppingCartApi.RemoveLineItem.class);
      return Reply.forward(removeItemCall.createCall(remove));
    } else {
      return Reply.failure("The operation [" + message.getOperation() + "] is not implemented.");
    }
  }
}
