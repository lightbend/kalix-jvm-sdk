package com.example.shoppingcart.domain;

import java.util.List;

public record ShoppingCart(String cartId, List<LineItem> items) {

  public ShoppingCart withItems(List<LineItem> items) {
    return new ShoppingCart(cartId, items);
  }
}
