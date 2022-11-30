package store.product.api;

import kalix.javasdk.testkit.KalixTestKit;
import kalix.javasdk.testkit.junit.KalixTestKitResource;
import org.junit.ClassRule;
import org.junit.Test;
import store.Main;

import static java.util.concurrent.TimeUnit.*;
import static org.junit.Assert.*;

// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
public class ProductValueEntityIntegrationTest {

  @ClassRule
  public static final KalixTestKitResource testKit =
      new KalixTestKitResource(
          Main.createKalix(), KalixTestKit.Settings.DEFAULT.withAdvancedViews());

  private final Products products;

  public ProductValueEntityIntegrationTest() {
    products = testKit.getGrpcClient(Products.class);
  }

  @Test
  public void createAndGetEntity() throws Exception {
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
}
