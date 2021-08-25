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

import com.akkaserverless.javasdk.impl.view.ViewException;
import com.akkaserverless.javasdk.impl.view.ViewHandler;
import com.akkaserverless.javasdk.view.UpdateContext;
import com.akkaserverless.javasdk.view.View;
import scala.Option;

// FIXME handwritten version for now (awaiting codegen)
public class ViewTckModelBehaviorHandler
    extends ViewHandler<com.akkaserverless.tck.model.View.ViewState, ViewTckModelBehavior> {

  public ViewTckModelBehaviorHandler(ViewTckModelBehavior view) {
    super(view);
  }

  @Override
  public View.UpdateEffect<com.akkaserverless.tck.model.View.ViewState> handleUpdate(
      String commandName,
      com.akkaserverless.tck.model.View.ViewState state,
      Object event,
      UpdateContext context) {
    switch (commandName) {
      case "ProcessUpdateUnary":
        return view().processUpdateUnary(state, (com.akkaserverless.tck.model.View.Event) event);

      default:
        throw new ViewException(
            context.viewId(),
            context.commandName(),
            "No command handler found for command ["
                + context.commandName()
                + "] on "
                + view().getClass().toString(),
            Option.empty());
    }
  }
}
