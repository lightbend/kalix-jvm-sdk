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

import com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound;
import com.akkaserverless.javasdk.impl.view.ViewRouter;
import com.akkaserverless.javasdk.view.View;

// FIXME handwritten version for now (awaiting codegen)
public class ViewTckModelBehaviorRouter
    extends ViewRouter<com.akkaserverless.tck.model.View.ViewState, ViewTckModelBehavior> {

  public ViewTckModelBehaviorRouter(ViewTckModelBehavior view) {
    super(view);
  }

  @Override
  public View.UpdateEffect<com.akkaserverless.tck.model.View.ViewState> handleUpdate(
      String eventName, com.akkaserverless.tck.model.View.ViewState state, Object event) {
    switch (eventName) {
      case "ProcessUpdateUnary":
        return view().processUpdateUnary(state, (com.akkaserverless.tck.model.View.Event) event);

      default:
        throw new UpdateHandlerNotFound(eventName);
    }
  }
}
