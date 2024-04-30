/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.eventsourcedentity;

import com.example.shoppingcart.ShoppingCartApi;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

/** An event sourced entity provider */
public class CartEntityProvider
    implements EventSourcedEntityProvider<ShoppingCartDomain.Cart, Object, CartEntity> {

  private final Function<EventSourcedEntityContext, CartEntity> entityFactory;
  private final EventSourcedEntityOptions options;

  /** Factory method of CartProvider */
  public static CartEntityProvider of(
      Function<EventSourcedEntityContext, CartEntity> entityFactory) {
    return new CartEntityProvider(entityFactory, EventSourcedEntityOptions.defaults());
  }

  private CartEntityProvider(
      Function<EventSourcedEntityContext, CartEntity> entityFactory,
      EventSourcedEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final EventSourcedEntityOptions options() {
    return options;
  }

  public final CartEntityProvider withOptions(EventSourcedEntityOptions options) {
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
  public final CartEntityRouter newRouter(EventSourcedEntityContext context) {
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
