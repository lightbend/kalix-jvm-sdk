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

package com.example.shoppingcart.domain;

import akka.grpc.ServiceDescription;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import com.akkaserverless.javasdk.lowlevel.EventSourcedEntityHandler;
import com.example.shoppingcart.ShoppingCartApi;
import com.google.protobuf.Descriptors;

import java.util.function.Function;

/**
 * Idea bout a generated descriptor/factory like thing that knows about entity type, descriptor etc
 * without having to create an instance of the entity first. It would allow simpler registration
 * something like: AkkaServerless .registerEventSourcedEntity(GeneratedFactory.forEntity(context ->
 * new ShopingCartImpl(context.entityId)))
 */
public final class GeneratedCartFactory {

  // some code here could likely go in an abstract factory base class which is what
  // AkkaServerless.register
  // knows about
  private final Descriptors.ServiceDescriptor serviceDescriptor;
  private final String entityType;
  private final Function<EventSourcedContext, GeneratedCartEntityBaseclass>
      userProvidedFactoryLambda;

  // maybe also options like, but user can change through .withSnapshotEvery(5).withOptions(options)
  private int snapshotEvery = 10;
  private EventSourcedEntityOptions options = EventSourcedEntityOptions.defaults();

  private GeneratedCartFactory(
      Function<EventSourcedContext, GeneratedCartEntityBaseclass> userProvidedFactoryLambda) {
    this.userProvidedFactoryLambda = userProvidedFactoryLambda;
    serviceDescriptor = ShoppingCartApi.getDescriptor().findServiceByName("ShoppingCartService");
    entityType = "ShoppingCart"; // FIXME from proto descriptor/name somehow?
  }

  // only user API taking lambda to create concrete entity instance
  static GeneratedCartFactory forEntity(
      Function<EventSourcedContext, GeneratedCartEntityBaseclass> createEntity) {
    return new GeneratedCartFactory(createEntity);
  }

  // internal method to create an actual handler instance
  GeneratedEventSourcedEntityHandler createHandler(EventSourcedContext context) {
    return new GeneratedEventSourcedEntityHandler(userProvidedFactoryLambda.apply(context));
  }
}
