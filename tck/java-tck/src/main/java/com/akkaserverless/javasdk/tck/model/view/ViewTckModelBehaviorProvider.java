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

import com.akkaserverless.javasdk.view.ViewCreationContext;
import com.akkaserverless.javasdk.view.ViewOptions;
import com.akkaserverless.javasdk.view.ViewProvider;
import com.akkaserverless.tck.model.View;
import com.google.protobuf.Descriptors;

import java.util.function.Function;

// FIXME handwritten version for now (awaiting codegen)
public class ViewTckModelBehaviorProvider
    implements ViewProvider<com.akkaserverless.tck.model.View.ViewState, ViewTckModelBehavior> {

  private final Function<ViewCreationContext, ViewTckModelBehavior> viewFactory;
  private final String viewId;
  private final ViewOptions options;

  /** Factory method of MyServiceProvider */
  public static ViewTckModelBehaviorProvider of(
      Function<ViewCreationContext, ViewTckModelBehavior> viewFactory) {
    return new ViewTckModelBehaviorProvider(viewFactory, "tck-view", ViewOptions.defaults());
  }

  private ViewTckModelBehaviorProvider(
      Function<ViewCreationContext, ViewTckModelBehavior> viewFactory,
      String viewId,
      ViewOptions options) {
    this.viewFactory = viewFactory;
    this.viewId = viewId;
    this.options = options;
  }

  @Override
  public String viewId() {
    return viewId;
  }

  @Override
  public ViewOptions options() {
    return options;
  }

  public ViewTckModelBehaviorProvider withOptions(ViewOptions options) {
    return new ViewTckModelBehaviorProvider(viewFactory, viewId, options);
  }

  @Override
  public final Descriptors.ServiceDescriptor serviceDescriptor() {
    return View.getDescriptor().findServiceByName("ViewTckModel");
  }

  @Override
  public final ViewTckModelBehaviorRouter newRouter(ViewCreationContext context) {
    return new ViewTckModelBehaviorRouter(viewFactory.apply(context));
  }

  @Override
  public final Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {View.getDescriptor()};
  }
}
