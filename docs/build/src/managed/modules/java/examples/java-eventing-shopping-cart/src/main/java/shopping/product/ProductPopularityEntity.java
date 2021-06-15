/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package shopping.product;

import com.akkaserverless.javasdk.EntityId;
import com.akkaserverless.javasdk.valueentity.CommandContext;
import com.akkaserverless.javasdk.valueentity.CommandHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.google.protobuf.Empty;
import shopping.product.api.ProductPopularityApi;
import shopping.product.domain.ProductPopularityDomain;

import java.util.Optional;

@ValueEntity(entityType = "product-popularity")
public class ProductPopularityEntity {

  private final String productId;

  public ProductPopularityEntity(@EntityId String productId) {
    this.productId = productId;
  }

  @CommandHandler
  public ProductPopularityApi.Popularity getPopularity(
      CommandContext<ProductPopularityDomain.Popularity> ctx) {
    return convert(getStateOrNew(ctx.getState()));
  }

  private ProductPopularityApi.Popularity convert(ProductPopularityDomain.Popularity popularity) {
    return ProductPopularityApi.Popularity.newBuilder()
        .setProductId(productId)
        .setScore(popularity.getScore())
        .build();
  }

  @CommandHandler
  public Empty increase(
      ProductPopularityApi.IncreasePopularity increase,
      CommandContext<ProductPopularityDomain.Popularity> ctx) {
    ProductPopularityDomain.Popularity.Builder builder = toBuilder(ctx.getState());
    int newScore = builder.getScore() + increase.getQuantity();
    builder.setProductId(increase.getProductId());
    ProductPopularityDomain.Popularity updated = builder.setScore(newScore).build();
    ctx.updateState(updated);
    return Empty.getDefaultInstance();
  }

  @CommandHandler
  public Empty decrease(
      ProductPopularityApi.DecreasePopularity decrease,
      CommandContext<ProductPopularityDomain.Popularity> ctx) {

    ProductPopularityDomain.Popularity.Builder builder = toBuilder(ctx.getState());
    int newScore = builder.getScore() - decrease.getQuantity();
    builder.setScore(newScore);
    ctx.updateState(builder.build());
    return Empty.getDefaultInstance();
  }

  private ProductPopularityDomain.Popularity.Builder toBuilder(
      Optional<ProductPopularityDomain.Popularity> state) {
    return state.map(s -> s.toBuilder()).orElse(newInstanceBuilder());
  }

  /** Either return the current state or create a new with popularity score set to 0 */
  private ProductPopularityDomain.Popularity getStateOrNew(
      Optional<ProductPopularityDomain.Popularity> state) {
    return state.orElse(newInstanceBuilder().build());
  }

  /**
   * Create a new ProductPopularity build with popularity score set to 0 and for the current
   * productId
   */
  private ProductPopularityDomain.Popularity.Builder newInstanceBuilder() {
    return ProductPopularityDomain.Popularity.newBuilder().setProductId(productId).setScore(0);
  }
}
