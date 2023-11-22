package store.product.api;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import store.Main;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.jupiter.api.Assertions.*;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class ProductEntityIntegrationTest {

  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(
          Main.createKalix(), KalixTestKit.Settings.DEFAULT.withAdvancedViews());

  private final Products products;

  public ProductEntityIntegrationTest() {
    products = testKit.getGrpcClient(Products.class);
  }

  @Test
  public void createAndGetProduct() throws Exception {
    ProductApi.Product product =
        ProductApi.Product.newBuilder()
            .setProductId("P123")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductApi.Money.newBuilder().setCurrency("USD").setUnits(123).setCents(45).build())
            .build();
    products.create(product).toCompletableFuture().get(5, SECONDS);
    ProductApi.Product result =
        products
            .get(ProductApi.GetProduct.newBuilder().setProductId("P123").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(product, result);
  }

  @Test
  public void changeProductName() throws Exception {
    ProductApi.Product product =
        ProductApi.Product.newBuilder()
            .setProductId("P234")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductApi.Money.newBuilder().setCurrency("USD").setUnits(123).setCents(45).build())
            .build();
    products.create(product).toCompletableFuture().get(5, SECONDS);
    ProductApi.Product result =
        products
            .get(ProductApi.GetProduct.newBuilder().setProductId("P234").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(product, result);

    String newName = "Thing Supreme";
    ProductApi.ChangeProductName changeProductName =
        ProductApi.ChangeProductName.newBuilder().setProductId("P234").setNewName(newName).build();
    products.changeName(changeProductName).toCompletableFuture().get(5, SECONDS);
    ProductApi.Product productWithNewName = product.toBuilder().setProductName(newName).build();
    ProductApi.Product updatedResult =
        products
            .get(ProductApi.GetProduct.newBuilder().setProductId("P234").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(productWithNewName, updatedResult);
  }

  @Test
  public void changeProductPrice() throws Exception {
    ProductApi.Product product =
        ProductApi.Product.newBuilder()
            .setProductId("P345")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductApi.Money.newBuilder().setCurrency("USD").setUnits(123).setCents(45).build())
            .build();
    products.create(product).toCompletableFuture().get(5, SECONDS);
    ProductApi.Product result =
        products
            .get(ProductApi.GetProduct.newBuilder().setProductId("P345").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(product, result);

    ProductApi.Money newPrice =
        ProductApi.Money.newBuilder().setCurrency("USD").setUnits(56).setCents(78).build();
    ProductApi.ChangeProductPrice changeProductPrice =
        ProductApi.ChangeProductPrice.newBuilder()
            .setProductId("P345")
            .setNewPrice(newPrice)
            .build();
    products.changePrice(changeProductPrice).toCompletableFuture().get(5, SECONDS);
    ProductApi.Product productWithNewPrice = product.toBuilder().setPrice(newPrice).build();
    ProductApi.Product updatedResult =
        products
            .get(ProductApi.GetProduct.newBuilder().setProductId("P345").build())
            .toCompletableFuture()
            .get(5, SECONDS);
    assertEquals(productWithNewPrice, updatedResult);
  }
}
