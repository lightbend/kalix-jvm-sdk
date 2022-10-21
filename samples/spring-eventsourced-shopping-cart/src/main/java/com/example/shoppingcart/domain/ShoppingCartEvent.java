package com.example.shoppingcart.domain;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// tag::events[]
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(value = ShoppingCartEvent.ItemAdded.class, name = "item-added"),
        @JsonSubTypes.Type(value = ShoppingCartEvent.ItemRemoved.class, name = "item-removed"),
        @JsonSubTypes.Type(value = ShoppingCartEvent.CheckedOut.class, name = "checked-out")
    })
public sealed interface ShoppingCartEvent {

  record ItemAdded(ShoppingCart.LineItem item) implements ShoppingCartEvent { }

  record ItemRemoved(String productId) implements ShoppingCartEvent { }

  record CheckedOut(int timestamp) implements ShoppingCartEvent { }
}
// end::events[]
