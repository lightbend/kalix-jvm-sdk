/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter;
import com.example.replicatedentity.shoppingcart.ShoppingCartApi;
import com.example.replicatedentity.shoppingcart.domain.ShoppingCartDomain;

public class CartEntityRouter
    extends ReplicatedEntityRouter<
        ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem>, CartEntity> {

  public CartEntityRouter(CartEntity entity) {
    super(entity);
  }

  @Override
  public ReplicatedEntity.Effect<?> handleCommand(
      String commandName,
      ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem> data,
      Object command,
      CommandContext context) {
    switch (commandName) {
      case "AddItem":
        return entity().addItem(data, (ShoppingCartApi.AddLineItem) command);
      case "RemoveItem":
        return entity().removeItem(data, (ShoppingCartApi.RemoveLineItem) command);
      case "GetCart":
        return entity().getCart(data, (ShoppingCartApi.GetShoppingCart) command);
      case "RemoveCart":
        return entity().removeCart(data, (ShoppingCartApi.RemoveShoppingCart) command);
      default:
        throw new ReplicatedEntityRouter.CommandHandlerNotFound(commandName);
    }
  }
}
