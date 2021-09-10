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

package com.akkaserverless.javasdk.tck.model.action;

import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.action.ActionProvider;
import com.akkaserverless.javasdk.impl.action.ActionHandler;
import com.akkaserverless.tck.model.Action;
import com.google.protobuf.Descriptors;

import java.util.function.Function;

public class ActionTckModelBehaviorProvider implements ActionProvider<ActionTckModelBehavior> {

  private final Function<ActionCreationContext, ActionTckModelBehavior> actionFactory;

  public static ActionTckModelBehaviorProvider of(
      Function<ActionCreationContext, ActionTckModelBehavior> actionFactory) {
    return new ActionTckModelBehaviorProvider(actionFactory);
  }

  private ActionTckModelBehaviorProvider(
      Function<ActionCreationContext, ActionTckModelBehavior> actionFactory) {
    this.actionFactory = actionFactory;
  }

  @Override
  public Descriptors.ServiceDescriptor serviceDescriptor() {
    return Action.getDescriptor().findServiceByName("ActionTckModel");
  }

  @Override
  public ActionTckModelBehaviorHandler newHandler(ActionCreationContext context) {
    return new ActionTckModelBehaviorHandler(actionFactory.apply(context));
  }

  @Override
  public Descriptors.FileDescriptor[] additionalDescriptors() {
    return new Descriptors.FileDescriptor[] {Action.getDescriptor()};
  }
}
