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

package com.akkaserverless.javasdk.action;

import com.akkaserverless.javasdk.impl.action.ActionHandler;
import com.google.protobuf.Descriptors;

/**
 * Register an Action in {{@link com.akkaserverless.javasdk.AkkaServerless}} using an <code>
 * ActionProvider</code>. The concrete <code>ActionProvider</code> is generated for the specific
 * entities defined in Protobuf, for example <code>CustomerActionProvider</code>.
 */
public interface ActionProvider<A extends Action> {
  // TODO: do we need/have ActionOptions?
  Descriptors.ServiceDescriptor serviceDescriptor();

  ActionHandler<A> newHandler(ActionCreationContext context);

  Descriptors.FileDescriptor[] additionalDescriptors();
}
