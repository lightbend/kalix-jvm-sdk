package store.product.domain;

// tag::domain[]
public record Product(String name, Money price) {
  public Product withName(String newName) {
    return new Product(newName, price);
  }

  public Product withPrice(Money newPrice) {
    return new Product(name, newPrice);
  }
}
// end::domain[]
