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

package shopping.cart;

import com.akkaserverless.javasdk.impl.view.ViewHandler;
import com.akkaserverless.javasdk.view.ViewCreationContext;
import com.akkaserverless.javasdk.view.ViewProvider;
import com.google.protobuf.Descriptors;
import shopping.cart.domain.ShoppingCartDomain;
import shopping.cart.view.ShoppingCartViewModel;

import java.util.function.Function;

// FIXME to be generated
public final class ShoppingCartViewProvider implements ViewProvider {

  private final Function<ViewCreationContext, ShoppingCartView> viewFactory;

  /** Factory method of MyServiceProvider */
  public static ShoppingCartViewProvider of(
      Function<ViewCreationContext, ShoppingCartView> viewFactory) {
    return new ShoppingCartViewProvider(viewFactory);
  }

  private ShoppingCartViewProvider(Function<ViewCreationContext, ShoppingCartView> viewFactory) {
    this.viewFactory = viewFactory;
  }

  @Override
  public String viewId() {
    return "carts";
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return ShoppingCartViewModel.getDescriptor().findServiceByName("ShoppingCartViewService");
  }

  @Override
  public final ViewHandler newHandler(ViewCreationContext context) {
    return new ShoppingCartViewHandler(viewFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {
      ShoppingCartViewModel.getDescriptor(), ShoppingCartDomain.getDescriptor()
    };
  }
}
