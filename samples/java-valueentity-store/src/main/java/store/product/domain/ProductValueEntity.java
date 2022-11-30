package store.product.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.valueentity.ValueEntityContext;
import store.product.api.ProductApi;

public class ProductValueEntity extends AbstractProductValueEntity {

  public ProductValueEntity(ValueEntityContext context) {}

  @Override
  public ProductDomain.ProductState emptyState() {
    return ProductDomain.ProductState.getDefaultInstance();
  }

  @Override
  public Effect<Empty> create(ProductDomain.ProductState currentState, ProductApi.Product product) {
    ProductDomain.ProductState productState =
        ProductDomain.ProductState.newBuilder()
            .setProductId(product.getProductId())
            .setProductName(product.getProductName())
            .setPrice(
                product.hasPrice()
                    ? ProductDomain.Money.newBuilder()
                        .setCurrency(product.getPrice().getCurrency())
                        .setUnits(product.getPrice().getUnits())
                        .setCents(product.getPrice().getCents())
                        .build()
                    : ProductDomain.Money.getDefaultInstance())
            .build();
    return effects().updateState(productState).thenReply(Empty.getDefaultInstance());
  }

  @Override
  public Effect<ProductApi.Product> get(
      ProductDomain.ProductState currentState, ProductApi.GetProduct getProduct) {
    ProductApi.Product product =
        ProductApi.Product.newBuilder()
            .setProductId(currentState.getProductId())
            .setProductName(currentState.getProductName())
            .setPrice(
                currentState.hasPrice()
                    ? ProductApi.Money.newBuilder()
                        .setCurrency(currentState.getPrice().getCurrency())
                        .setUnits(currentState.getPrice().getUnits())
                        .setCents(currentState.getPrice().getCents())
                        .build()
                    : ProductApi.Money.getDefaultInstance())
            .build();
    return effects().reply(product);
  }
}
