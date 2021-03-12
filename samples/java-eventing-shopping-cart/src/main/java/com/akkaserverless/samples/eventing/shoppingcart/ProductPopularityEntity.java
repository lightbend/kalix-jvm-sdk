/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples.eventing.shoppingcart;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.google.protobuf.Empty;
import shopping.product.api.ProductApi;
import shopping.product.model.Product;

import java.util.Optional;

@ValueEntity(entityType = "product-popularity")
public class ProductPopularityEntity {

  private final String productId;

  public ProductPopularityEntity(@EntityId String productId) {
    this.productId = productId;
  }

  @CommandHandler
  public ProductApi.Popularity getPopularity(CommandContext<Product.Popularity> ctx) {
    return convert(getStateOrNew(ctx.getState()));
  }

  private ProductApi.Popularity convert(Product.Popularity popularity) {

    return ProductApi.Popularity.newBuilder()
        .setProductId(productId)
        .setScore(popularity.getScore())
        .build();
  }

  @CommandHandler
  public Empty increase(
      ProductApi.IncreasePopularity increase, CommandContext<Product.Popularity> ctx) {
    Product.Popularity.Builder builder = toBuilder(ctx.getState());
    int newScore = builder.getScore() + increase.getQuantity();
    builder.setProductId(increase.getProductId());
    Product.Popularity updated = builder.setScore(newScore).build();
    ctx.updateState(updated);
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty decrease(
      ProductApi.DecreasePopularity decrease, CommandContext<Product.Popularity> ctx) {

    Product.Popularity.Builder builder = toBuilder(ctx.getState());
    int newScore = builder.getScore() - decrease.getQuantity();
    builder.setScore(newScore);
    ctx.updateState(builder.build());
    return Empty.getDefaultInstance();
  }

  private Product.Popularity.Builder toBuilder(Optional<Product.Popularity> state) {
    return state.map(s -> s.toBuilder()).orElse(newInstanceBuilder());
  }

  /** Either return the current state or create a new with popularity score set to 0 */
  private Product.Popularity getStateOrNew(Optional<Product.Popularity> state) {
    return state.orElse(newInstanceBuilder().build());
  }

  /**
   * Create a new ProductPopularity build with popularity score set to 0 and for the current
   * productId
   */
  private Product.Popularity.Builder newInstanceBuilder() {
    return Product.Popularity.newBuilder().setProductId(productId).setScore(0);
  }
}
