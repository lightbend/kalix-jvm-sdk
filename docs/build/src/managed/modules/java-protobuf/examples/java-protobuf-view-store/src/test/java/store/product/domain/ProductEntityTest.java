package store.product.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.EventSourcedResult;
import org.junit.jupiter.api.Test;
import store.product.api.ProductApi;

import static org.junit.jupiter.api.Assertions.*;

public class ProductEntityTest {

  @Test
  public void createAndGetTest() {
    ProductEntityTestKit service = ProductEntityTestKit.of(ProductEntity::new);

    ProductApi.Product product =
        ProductApi.Product.newBuilder()
            .setProductId("P123")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductApi.Money.newBuilder().setCurrency("USD").setUnits(123).setCents(45).build())
            .build();

    ProductDomain.ProductState productState =
        ProductDomain.ProductState.newBuilder()
            .setProductId("P123")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductDomain.Money.newBuilder()
                    .setCurrency("USD")
                    .setUnits(123)
                    .setCents(45)
                    .build())
            .build();

    EventSourcedResult<Empty> createResult = service.create(product);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());

    assertEquals(1, createResult.getAllEvents().size());
    ProductDomain.ProductCreated productCreated =
        createResult.getNextEventOfType(ProductDomain.ProductCreated.class);
    assertEquals(productState, productCreated.getProduct());

    assertEquals(productState, service.getState());

    EventSourcedResult<ProductApi.Product> getResult =
        service.get(ProductApi.GetProduct.newBuilder().setProductId("P123").build());
    assertEquals(product, getResult.getReply());
  }

  @Test
  public void changeNameTest() {
    ProductEntityTestKit service = ProductEntityTestKit.of(ProductEntity::new);

    ProductApi.Product product =
        ProductApi.Product.newBuilder()
            .setProductId("P123")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductApi.Money.newBuilder().setCurrency("USD").setUnits(123).setCents(45).build())
            .build();

    EventSourcedResult<Empty> createResult = service.create(product);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());

    ProductDomain.ProductState productState =
        ProductDomain.ProductState.newBuilder()
            .setProductId("P123")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductDomain.Money.newBuilder()
                    .setCurrency("USD")
                    .setUnits(123)
                    .setCents(45)
                    .build())
            .build();

    assertEquals(productState, service.getState());

    ProductApi.ChangeProductName changeProductName =
        ProductApi.ChangeProductName.newBuilder()
            .setProductId("P123")
            .setNewName("Thing Supreme")
            .build();

    EventSourcedResult<Empty> changeNameResult = service.changeName(changeProductName);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());

    assertEquals(1, changeNameResult.getAllEvents().size());
    ProductDomain.ProductNameChanged productNameChanged =
        changeNameResult.getNextEventOfType(ProductDomain.ProductNameChanged.class);
    assertEquals("Thing Supreme", productNameChanged.getNewName());

    ProductDomain.ProductState productStateWithNewName =
        productState.toBuilder().setProductName("Thing Supreme").build();
    assertEquals(productStateWithNewName, service.getState());

    ProductApi.Product productWithNewName =
        product.toBuilder().setProductName("Thing Supreme").build();

    EventSourcedResult<ProductApi.Product> getResult =
        service.get(ProductApi.GetProduct.newBuilder().setProductId("P123").build());
    assertEquals(productWithNewName, getResult.getReply());
  }

  @Test
  public void changePriceTest() {
    ProductEntityTestKit service = ProductEntityTestKit.of(ProductEntity::new);

    ProductApi.Product product =
        ProductApi.Product.newBuilder()
            .setProductId("P123")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductApi.Money.newBuilder().setCurrency("USD").setUnits(123).setCents(45).build())
            .build();

    EventSourcedResult<Empty> createResult = service.create(product);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());

    ProductDomain.ProductState productState =
        ProductDomain.ProductState.newBuilder()
            .setProductId("P123")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductDomain.Money.newBuilder()
                    .setCurrency("USD")
                    .setUnits(123)
                    .setCents(45)
                    .build())
            .build();

    assertEquals(productState, service.getState());

    ProductApi.ChangeProductPrice changeProductPrice =
        ProductApi.ChangeProductPrice.newBuilder()
            .setProductId("P123")
            .setNewPrice(
                ProductApi.Money.newBuilder().setCurrency("USD").setUnits(56).setCents(78).build())
            .build();

    EventSourcedResult<Empty> changePriceResult = service.changePrice(changeProductPrice);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());

    assertEquals(1, changePriceResult.getAllEvents().size());
    ProductDomain.ProductPriceChanged productPriceChanged =
        changePriceResult.getNextEventOfType(ProductDomain.ProductPriceChanged.class);
    assertEquals("USD", productPriceChanged.getNewPrice().getCurrency());
    assertEquals(56, productPriceChanged.getNewPrice().getUnits());
    assertEquals(78, productPriceChanged.getNewPrice().getCents());

    ProductDomain.ProductState productStateWithNewPrice =
        productState.toBuilder()
            .setPrice(
                ProductDomain.Money.newBuilder()
                    .setCurrency("USD")
                    .setUnits(56)
                    .setCents(78)
                    .build())
            .build();
    assertEquals(productStateWithNewPrice, service.getState());

    ProductApi.Product productWithNewPrice =
        product.toBuilder()
            .setPrice(
                ProductApi.Money.newBuilder().setCurrency("USD").setUnits(56).setCents(78).build())
            .build();

    EventSourcedResult<ProductApi.Product> getResult =
        service.get(ProductApi.GetProduct.newBuilder().setProductId("P123").build());
    assertEquals(productWithNewPrice, getResult.getReply());
  }
}
