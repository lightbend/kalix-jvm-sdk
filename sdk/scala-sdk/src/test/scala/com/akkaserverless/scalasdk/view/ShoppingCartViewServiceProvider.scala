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

package com.akkaserverless.scalasdk.view

import com.akkaserverless.javasdk.shoppingcart.ShoppingCart
import com.akkaserverless.javasdk.shoppingcart.ShoppingCartViewModel
import com.akkaserverless.scalasdk.impl.view.ViewHandler
import com.google.protobuf.Descriptors
import com.google.protobuf.EmptyProto

// FIXME this can be removed, unless it evolves into usage in tests

object ShoppingCartViewServiceProvider {
  def apply(viewFactory: ViewCreationContext => ShoppingCartViewServiceImpl): ShoppingCartViewServiceProvider =
    new ShoppingCartViewServiceProvider(viewFactory)
}

class ShoppingCartViewServiceProvider private (viewFactory: ViewCreationContext => ShoppingCartViewServiceImpl)
    extends ViewProvider[ShoppingCartViewModel.CartViewState, ShoppingCartViewServiceImpl] {

  override def serviceDescriptor: Descriptors.ServiceDescriptor =
    ShoppingCartViewModel.getDescriptor.findServiceByName("ShoppingCartViewService")

  override def viewId: String =
    "ShoppingCartViewService"

  override def newHandler(
      context: ViewCreationContext): ViewHandler[ShoppingCartViewModel.CartViewState, ShoppingCartViewServiceImpl] =
    new ShoppingCartViewServiceHandler(viewFactory(context))

  override def additionalDescriptors(): Seq[Descriptors.FileDescriptor] =
    ShoppingCartViewModel.getDescriptor ::
    ShoppingCart.getDescriptor ::
    EmptyProto.getDescriptor ::
    Nil
}
