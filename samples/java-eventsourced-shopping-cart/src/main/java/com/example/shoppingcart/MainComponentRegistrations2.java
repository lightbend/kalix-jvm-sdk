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

package com.example.shoppingcart;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import com.akkaserverless.javasdk.lowlevel.EventSourcedEntityFactory;
import com.akkaserverless.javasdk.lowlevel.EventSourcedEntityHandler;
import com.example.shoppingcart.domain.GeneratedEventSourcedEntityHandler;
import com.example.shoppingcart.domain.ShoppingCartDomain;
import com.example.shoppingcart.domain.ShoppingCartImpl;
import com.google.protobuf.EmptyProto;

public final class MainComponentRegistrations2 {

  public static AkkaServerless withGeneratedComponentsAdded(AkkaServerless akkaServerless) {
    return akkaServerless
        .lowLevel()
        .registerEventSourcedEntity(
            // see GeneratedFactory for additional idea
            context ->
                new GeneratedEventSourcedEntityHandler(new ShoppingCartImpl(context.entityId())),
            ShoppingCartApi.getDescriptor().findServiceByName("ShoppingCartService"),
            // entity type
            ShoppingCartImpl.class.getSimpleName(),
            // snapshot every
            10,
            // options
            EventSourcedEntityOptions.defaults());
  }
}
