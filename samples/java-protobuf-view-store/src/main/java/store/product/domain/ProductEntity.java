package store.product.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import store.product.api.ProductApi;

public class ProductEntity extends AbstractProductEntity {

  public ProductEntity(EventSourcedEntityContext context) {}

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
    ProductDomain.ProductCreated productCreated =
        ProductDomain.ProductCreated.newBuilder().setProduct(productState).build();
    return effects().emitEvent(productCreated).thenReply(__ -> Empty.getDefaultInstance());
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

  @Override
  public Effect<Empty> changeName(
      ProductDomain.ProductState currentState, ProductApi.ChangeProductName changeProductName) {
    ProductDomain.ProductNameChanged productNameChanged =
        ProductDomain.ProductNameChanged.newBuilder()
            .setNewName(changeProductName.getNewName())
            .build();
    return effects().emitEvent(productNameChanged).thenReply(__ -> Empty.getDefaultInstance());
  }

  @Override
  public Effect<Empty> changePrice(
      ProductDomain.ProductState currentState, ProductApi.ChangeProductPrice changeProductPrice) {
    ProductDomain.ProductPriceChanged productPriceChanged =
        ProductDomain.ProductPriceChanged.newBuilder()
            .setNewPrice(
                changeProductPrice.hasNewPrice()
                    ? ProductDomain.Money.newBuilder()
                        .setCurrency(changeProductPrice.getNewPrice().getCurrency())
                        .setUnits(changeProductPrice.getNewPrice().getUnits())
                        .setCents(changeProductPrice.getNewPrice().getCents())
                        .build()
                    : ProductDomain.Money.getDefaultInstance())
            .build();
    return effects().emitEvent(productPriceChanged).thenReply(__ -> Empty.getDefaultInstance());
  }

  @Override
  public ProductDomain.ProductState productCreated(
      ProductDomain.ProductState currentState, ProductDomain.ProductCreated productCreated) {
    return productCreated.getProduct();
  }

  @Override
  public ProductDomain.ProductState productNameChanged(
      ProductDomain.ProductState currentState,
      ProductDomain.ProductNameChanged productNameChanged) {
    return currentState.toBuilder().setProductName(productNameChanged.getNewName()).build();
  }

  @Override
  public ProductDomain.ProductState productPriceChanged(
      ProductDomain.ProductState currentState,
      ProductDomain.ProductPriceChanged productPriceChanged) {
    return currentState.toBuilder().setPrice(productPriceChanged.getNewPrice()).build();
  }
}
