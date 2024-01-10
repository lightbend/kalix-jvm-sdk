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
