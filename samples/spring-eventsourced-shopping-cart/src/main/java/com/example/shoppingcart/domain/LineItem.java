package com.example.shoppingcart.domain;

public record LineItem(String productId, String name, int quantity) {
  public LineItem withQuantity(int quantity) {
    return new LineItem(productId, name, quantity);
  }
}
