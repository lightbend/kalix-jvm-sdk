package store.product.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ValueEntityResult;
import org.junit.Test;
import store.product.api.ProductApi;

import static org.junit.Assert.*;

public class ProductValueEntityTest {

  @Test
  public void createAndGetTest() {
    ProductValueEntityTestKit service = ProductValueEntityTestKit.of(ProductValueEntity::new);
    ProductApi.Product product =
        ProductApi.Product.newBuilder()
            .setProductId("P123")
            .setProductName("Super Duper Thingamajig")
            .setPrice(
                ProductApi.Money.newBuilder().setCurrency("USD").setUnits(123).setCents(45).build())
            .build();
    ValueEntityResult<Empty> createResult = service.create(product);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());
    ProductDomain.ProductState currentState = service.getState();
    assertEquals("P123", currentState.getProductId());
    assertEquals("Super Duper Thingamajig", currentState.getProductName());
    assertTrue(currentState.hasPrice());
    assertEquals("USD", currentState.getPrice().getCurrency());
    assertEquals(123, currentState.getPrice().getUnits());
    assertEquals(45, currentState.getPrice().getCents());
    ValueEntityResult<ProductApi.Product> getResult =
        service.get(ProductApi.GetProduct.newBuilder().setProductId("P123").build());
    assertEquals(product, getResult.getReply());
  }
}
