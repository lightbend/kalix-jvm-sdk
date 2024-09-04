package store.product.api;

import store.product.domain.Money;
import store.product.domain.Product;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.annotations.EventHandler;
import org.springframework.web.bind.annotation.*;
import store.product.domain.ProductEvent;

import static store.product.domain.ProductEvent.*;

@TypeId("product")
@Id("id")
@RequestMapping("/product/{id}")
public class ProductEntity extends EventSourcedEntity<Product, ProductEvent> {

  @GetMapping
  public Effect<Product> get() {
    return effects().reply(currentState());
  }

  @PostMapping("/create")
  public Effect<String> create(@RequestBody Product product) {
    return effects()
        .emitEvent(new ProductCreated(product.name(), product.price()))
        .thenReply(__ -> "OK");
  }

  @EventHandler
  public Product onEvent(ProductCreated created) {
    return new Product(created.name(), created.price());
  }

  @PostMapping("/changeName/{newName}")
  public Effect<String> changeName(@PathVariable String newName) {
    return effects().emitEvent(new ProductNameChanged(newName)).thenReply(__ -> "OK");
  }

  @EventHandler
  public Product onEvent(ProductNameChanged productNameChanged) {
    return currentState().withName(productNameChanged.newName());
  }

  @PostMapping("/changePrice")
  public Effect<String> changePrice(@RequestBody Money newPrice) {
    return effects().emitEvent(new ProductPriceChanged(newPrice)).thenReply(__ -> "OK");
  }

  @EventHandler
  public Product onEvent(ProductPriceChanged productPriceChanged) {
    return currentState().withPrice(productPriceChanged.newPrice());
  }
}
