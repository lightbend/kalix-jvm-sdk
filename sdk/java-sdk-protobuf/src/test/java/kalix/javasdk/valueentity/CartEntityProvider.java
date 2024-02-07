/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.valueentity;

import com.example.valueentity.shoppingcart.ShoppingCartApi;
import com.example.valueentity.shoppingcart.domain.ShoppingCartDomain;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

/** A value entity provider */
public class CartEntityProvider
    implements ValueEntityProvider<ShoppingCartDomain.Cart, CartEntity> {

  private final Function<ValueEntityContext, CartEntity> entityFactory;
  private final ValueEntityOptions options;

  /** Factory method of ShoppingCartProvider */
  public static CartEntityProvider of(Function<ValueEntityContext, CartEntity> entityFactory) {
    return new CartEntityProvider(entityFactory, ValueEntityOptions.defaults());
  }

  private CartEntityProvider(
      Function<ValueEntityContext, CartEntity> entityFactory, ValueEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ValueEntityOptions options() {
    return options;
  }

  public final CartEntityProvider withOptions(ValueEntityOptions options) {
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
  public final CartEntityRouter newRouter(ValueEntityContext context) {
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
