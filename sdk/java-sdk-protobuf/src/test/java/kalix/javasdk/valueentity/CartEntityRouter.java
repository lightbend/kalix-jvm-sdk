/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.valueentity;

import kalix.javasdk.impl.valueentity.ValueEntityRouter;
import com.example.valueentity.shoppingcart.ShoppingCartApi;
import com.example.valueentity.shoppingcart.domain.ShoppingCartDomain;

/** A value entity handler */
public class CartEntityRouter extends ValueEntityRouter<ShoppingCartDomain.Cart, CartEntity> {

  public CartEntityRouter(CartEntity entity) {
    super(entity);
  }

  @Override
  public ValueEntity.Effect<?> handleCommand(
      String commandName, ShoppingCartDomain.Cart state, Object command, CommandContext context) {
    switch (commandName) {
      case "AddItem":
        return entity().addItem(state, (ShoppingCartApi.AddLineItem) command);
      case "RemoveItem":
        return entity().removeItem(state, (ShoppingCartApi.RemoveLineItem) command);
      case "GetCart":
        return entity().getCart(state, (ShoppingCartApi.GetShoppingCart) command);
      case "RemoveCart":
        return entity().removeCart(state, (ShoppingCartApi.RemoveShoppingCart) command);
      default:
        throw new ValueEntityRouter.CommandHandlerNotFound(commandName);
    }
  }
}
