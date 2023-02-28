package store.product.api;

import kalix.javasdk.testkit.EventSourcedResult;
import kalix.javasdk.testkit.EventSourcedTestKit;
import org.junit.jupiter.api.Test;
import store.product.domain.Money;
import store.product.domain.Product;
import store.product.domain.ProductEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ProductEntityTest {

  @Test
  public void testProductNameChange() {

    EventSourcedTestKit<Product, ProductEvent, ProductEntity> testKit =
        EventSourcedTestKit.of(ProductEntity::new);

    {
      String name = "Super Duper Thingamajig";
      Product product = new Product(name, new Money("USD", 123, 45));
      EventSourcedResult<String> result = testKit.call(entity -> entity.create(product));
      assertEquals("OK", result.getReply());
      assertEquals(name, testKit.getState().name());
      result.getNextEventOfType(ProductEvent.ProductCreated.class);
    }

    {
      String newName = "Thing Supreme";
      EventSourcedResult<String> result = testKit.call(entity -> entity.changeName(newName));
      assertEquals("OK", result.getReply());
      assertEquals(newName, testKit.getState().name());
      result.getNextEventOfType(ProductEvent.ProductNameChanged.class);
    }
  }

  @Test
  public void testProductPriceChange() {

    EventSourcedTestKit<Product, ProductEvent, ProductEntity> testKit =
        EventSourcedTestKit.of(ProductEntity::new);

    {
      Money price = new Money("USD", 123, 45);
      Product product = new Product("Super Duper Thingamajig", price);
      EventSourcedResult<String> result = testKit.call(e -> e.create(product));
      assertEquals("OK", result.getReply());
      assertEquals(price.currency(), testKit.getState().price().currency());
      assertEquals(price.units(), testKit.getState().price().units());
      assertEquals(price.cents(), testKit.getState().price().cents());
      result.getNextEventOfType(ProductEvent.ProductCreated.class);
    }

    {
      Money newPrice = new Money("USD", 56, 78);
      EventSourcedResult<String> result = testKit.call(e -> e.changePrice(newPrice));
      assertEquals("OK", result.getReply());
      assertEquals(newPrice.currency(), testKit.getState().price().currency());
      assertEquals(newPrice.units(), testKit.getState().price().units());
      assertEquals(newPrice.cents(), testKit.getState().price().cents());
      result.getNextEventOfType(ProductEvent.ProductPriceChanged.class);
    }
  }
}
