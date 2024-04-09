/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.replicatedentity;

import com.example.replicatedentity.shoppingcart.ShoppingCartApi;
import com.example.replicatedentity.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

public class CartEntityProvider
    implements ReplicatedEntityProvider<
        ReplicatedRegisterMap<String, ShoppingCartDomain.LineItem>, CartEntity> {

  private final Function<ReplicatedEntityContext, CartEntity> entityFactory;
  private final ReplicatedEntityOptions options;

  public static CartEntityProvider of(Function<ReplicatedEntityContext, CartEntity> entityFactory) {
    return new CartEntityProvider(entityFactory, ReplicatedEntityOptions.defaults());
  }

  private CartEntityProvider(
      Function<ReplicatedEntityContext, CartEntity> entityFactory,
      ReplicatedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ReplicatedEntityOptions options() {
    return options;
  }

  public final CartEntityProvider withOptions(ReplicatedEntityOptions options) {
    return new CartEntityProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return ShoppingCartApi.getDescriptor().findServiceByName("ShoppingCartService");
  }

  @Override
  public final String typeId() {
    return "shopping-cart";
  }

  @Override
  public final CartEntityRouter newRouter(ReplicatedEntityContext context) {
    return new CartEntityRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      ShoppingCartApi.getDescriptor(),
      ShoppingCartDomain.getDescriptor(),
      EmptyProto.getDescriptor()
    };
  }
}
