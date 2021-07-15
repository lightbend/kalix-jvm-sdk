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

package com.akkaserverless.javasdk.eventsourcedentity;

import com.akkaserverless.javasdk.impl.eventsourcedentity.AbstractEventSourcedEntityHandler;
import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;

/** Generated, does the routing from command name to concrete method */
final class CartHandler
    extends AbstractEventSourcedEntityHandler<ShoppingCartDomain.Cart, CartEntity> {

  public CartHandler(CartEntity entity) {
    super(entity);
  }

  @Override
  public ShoppingCartDomain.Cart handleEvent(ShoppingCartDomain.Cart state, Object event) {
    if (event instanceof ShoppingCartDomain.ItemAdded) {
      return entity().itemAdded(state, (ShoppingCartDomain.ItemAdded) event);
    } else if (event instanceof ShoppingCartDomain.ItemRemoved) {
      return entity().itemRemoved(state, (ShoppingCartDomain.ItemRemoved) event);
    } else {
      throw new IllegalArgumentException("Unknown event type [" + event.getClass() + "]");
    }
  }

  @Override
  public EventSourcedEntityBase.Effect<?> handleCommand(
      String commandName, ShoppingCartDomain.Cart state, Any command) {
    try {
      switch (commandName) {
        case "AddItem":
          // FIXME could parsing to the right type also be pulled out of here?
          return entity().addItem(state, ShoppingCartApi.AddLineItem.parseFrom(command.getValue()));
        case "RemoveItem":
          return entity()
              .removeItem(state, ShoppingCartApi.RemoveLineItem.parseFrom(command.getValue()));
        case "GetCart":
          return entity()
              .getCart(state, ShoppingCartApi.GetShoppingCart.parseFrom(command.getValue()));
        default:
          // FIXME signal this to the parent with less code
          throw new RuntimeException(
              "No command handler found for command ["
                  + commandName
                  + "] on "
                  + entity().getClass().toString());
          /*
          FIXME used to be this but that requires knowing _so_ much about context, we can decorate with that further "out"?
          throw new EntityExceptions.EntityException(
          context.entityId(),
          context.commandId(),
          commandName,
          "No command handler found for command ["
              + context.commandName()
              + "] on "
              + entity().getClass().toString()); */
      }
    } catch (InvalidProtocolBufferException ex) {
      // This is if command payload cannot be parsed
      throw new RuntimeException(ex);
    }
  }
}
