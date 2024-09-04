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
package com.example.api;

import com.example.api.ShoppingCartDTO;
import com.example.api.ShoppingCartDTO.LineItemDTO;
import com.example.domain.ShoppingCart;
import io.grpc.Status;
import kalix.javasdk.annotations.ForwardHeaders;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

/**
 * A value entity.
 */
// tag::summary[]
@Id("cartId")
@TypeId("shopping-cart")
@RequestMapping("/cart/{cartId}") // <1>
// end::summary[]
@ForwardHeaders("Role")
// tag::summary[]
public class ShoppingCartEntity extends ValueEntity<ShoppingCart> {
  // end::summary[]
  @SuppressWarnings("unused")
  private final String entityId;

  public ShoppingCartEntity(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public ShoppingCart emptyState() {
    return ShoppingCart.of(entityId);
  }

  // tag::create[]
  // tag::summary[]

  @PostMapping("/create") // <2>
  public ValueEntity.Effect<ShoppingCartDTO> create() {
    //...
    // end::summary[]
    if (currentState().creationTimestamp() > 0L) {
      return effects().error("Cart was already created");
    } else {
      var newState = currentState().withCreationTimestamp(Instant.now().toEpochMilli());
      return effects()
        .updateState(newState)
        .thenReply(ShoppingCartDTO.of(newState));
    }
  }
  // end::create[]

  // tag::add-item[]
  // tag::summary[]

  @PostMapping("/items/add") // <3>
  public ValueEntity.Effect<ShoppingCartDTO> addItem(@RequestBody LineItemDTO addLineItem) {
    //...
    // end::summary[]
    if (addLineItem.quantity() <= 0) {
      return effects()
          .error("Quantity for item " + addLineItem.productId() + " must be greater than zero.");
    }

    var newState = currentState().withItem(addLineItem.toDomain());
    return effects()
        .updateState(newState)
        .thenReply(ShoppingCartDTO.of(newState));
  }

  // end::add-item[]
  @PostMapping("/items/{productId}/remove")
  public ValueEntity.Effect<ShoppingCartDTO> removeItem(@PathVariable String productId) {
    var lineItemOpt = currentState().findItemByProductId(productId);

    if (lineItemOpt.isEmpty()) {
      return effects()
          .error("Cannot remove item " + productId + " because it is not in the cart.");
    }

    var newState = currentState().withoutItem(lineItemOpt.get());
    return effects()
        .updateState(newState)
        .thenReply(ShoppingCartDTO.of(newState));
  }

  // tag::get-cart[]
  // tag::summary[]

  @GetMapping // <4>
  public ValueEntity.Effect<ShoppingCartDTO> getCart() {
    //...
    // end::summary[]
    return effects().reply(ShoppingCartDTO.of(currentState()));
  }
  // end::get-cart[]

  @PostMapping("/remove")
  public ValueEntity.Effect<String> removeCart() {
    var userRole = commandContext().metadata().get("Role").get();
    if (userRole.equals("Admin")) {
      return effects().deleteEntity().thenReply("OK");
    } else {
      return effects().error("Only admin can remove the cart");
    }
  }
// tag::summary[]
}
// end::summary[]

