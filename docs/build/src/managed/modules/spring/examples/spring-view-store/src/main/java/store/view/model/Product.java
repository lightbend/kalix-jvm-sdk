package store.view.model;

import store.product.domain.Money;

public record Product(String productId, String productName, Money price) {

  public Product withProductName(String newProductName) {
    return new Product(productId, newProductName, price);
  }

  public Product withPrice(Money newPrice) {
    return new Product(productId, productName, newPrice);
  }
}
