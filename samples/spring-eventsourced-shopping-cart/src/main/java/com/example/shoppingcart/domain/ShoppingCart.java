package com.example.shoppingcart.domain;

import java.util.List;

public record ShoppingCart(String cartId, List<LineItem> items) {

  public record LineItem(String productId, String name, int quantity) {
    public LineItem withQuantity(int quantity) {
      return new LineItem(productId, name, quantity);
    }
  }

  public ShoppingCart withItems(List<LineItem> items) {
    return new ShoppingCart(cartId, items);
  }
}
