package store.product.domain;

import kalix.javasdk.annotations.TypeName;

public sealed interface ProductEvent {
  @TypeName("product-created")
  record ProductCreated(String name, Money price) implements ProductEvent {}

  @TypeName("product-name-changed")
  record ProductNameChanged(String newName) implements ProductEvent {}

  @TypeName("product-price-changed")
  record ProductPriceChanged(Money newPrice) implements ProductEvent {}
}
