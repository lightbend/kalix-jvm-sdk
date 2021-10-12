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

package com.akkaserverless.javasdk.tck.model.view;

import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.akkaserverless.javasdk.valueentity.ValueEntityProvider;
import com.akkaserverless.tck.model.View;
import com.google.protobuf.Descriptors;
import com.google.protobuf.EmptyProto;

import java.util.function.Function;

/** A value entity provider */
public class ViewTckSourceEntityProvider
    implements ValueEntityProvider<String, ViewTckSourceEntity> {

  private final Function<ValueEntityContext, ViewTckSourceEntity> entityFactory;
  private final ValueEntityOptions options;

  /** Factory method of ShoppingCartProvider */
  public static ViewTckSourceEntityProvider of(
      Function<ValueEntityContext, ViewTckSourceEntity> entityFactory) {
    return new ViewTckSourceEntityProvider(entityFactory, ValueEntityOptions.defaults());
  }

  private ViewTckSourceEntityProvider(
      Function<ValueEntityContext, ViewTckSourceEntity> entityFactory, ValueEntityOptions options) {
    this.entityFactory = entityFactory;
    this.options = options;
  }

  @Override
  public final ValueEntityOptions options() {
    return options;
  }

  public final ViewTckSourceEntityProvider withOptions(ValueEntityOptions options) {
    return new ViewTckSourceEntityProvider(entityFactory, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return View.getDescriptor().findServiceByName("ViewTckSource");
  }

  @Override
  public final String entityType() {
    return "view-source";
  }

  @Override
  public final ViewTckSourceEntityRouter newRouter(ValueEntityContext context) {
    return new ViewTckSourceEntityRouter(entityFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {View.getDescriptor(), EmptyProto.getDescriptor()};
  }
}
