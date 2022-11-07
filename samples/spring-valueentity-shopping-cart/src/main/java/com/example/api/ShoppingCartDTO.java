package com.example.api;

import com.example.domain.ShoppingCart;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// tag::domain[]
public record ShoppingCartDTO(String cartId, List<LineItemDTO> items) {

  public record LineItemDTO(String productId, String name, int quantity) {

    public ShoppingCart.LineItem toDomain() {
      return new ShoppingCart.LineItem(productId, name, quantity);
    }
  }

  public static ShoppingCartDTO of(ShoppingCart cart) {
    List<LineItemDTO> allItems =
        cart.items().stream()
            .map(i -> new LineItemDTO(i.productId(), i.name(), i.quantity()))
            .sorted(Comparator.comparing(LineItemDTO::productId))
            .toList();

    return new ShoppingCartDTO(cart.cartId(), allItems);
  }
}
// end::domain[]