/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.eventsourcedentity;

import kalix.javasdk.impl.eventsourcedentity.EventSourcedEntityRouter;
import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCartDomain;

/** Generated, does the routing from command name to concrete method */
final class CartEntityRouter extends EventSourcedEntityRouter<ShoppingCartDomain.Cart, Object, CartEntity> {

  public CartEntityRouter(CartEntity entity) {
    super(entity);
  }

  @Override
  public ShoppingCartDomain.Cart handleEvent(ShoppingCartDomain.Cart state, Object event) {
    if (event instanceof ShoppingCartDomain.ItemAdded) {
      return entity().itemAdded(state, (ShoppingCartDomain.ItemAdded) event);
    } else if (event instanceof ShoppingCartDomain.ItemRemoved) {
      return entity().itemRemoved(state, (ShoppingCartDomain.ItemRemoved) event);
    } else {
      throw new EventSourcedEntityRouter.EventHandlerNotFound(event.getClass());
    }
  }

  @Override
  public EventSourcedEntity.Effect<?> handleCommand(
      String commandName, ShoppingCartDomain.Cart state, Object command, CommandContext context) {
    switch (commandName) {
      case "AddItem":
        return entity().addItem(state, (ShoppingCartApi.AddLineItem) command);
      case "AddItems":
        return entity().addItems(state, (ShoppingCartApi.AddLineItems) command);
      case "RemoveItem":
        return entity().removeItem(state, (ShoppingCartApi.RemoveLineItem) command);
      case "GetCart":
        return entity().getCart(state, (ShoppingCartApi.GetShoppingCart) command);
      default:
        throw new EventSourcedEntityRouter.CommandHandlerNotFound(commandName);
    }
  }
}
