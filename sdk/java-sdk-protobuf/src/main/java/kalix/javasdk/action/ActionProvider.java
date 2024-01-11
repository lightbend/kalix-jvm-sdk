/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.action;

import com.google.protobuf.Descriptors;
import kalix.javasdk.Kalix;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.action.ActionRouter;

import java.util.Optional;

/**
 * Register an Action in {{@link Kalix}} using an <code>
 * ActionProvider</code>. The concrete <code>ActionProvider</code> is generated for the specific
 * entities defined in Protobuf, for example <code>CustomerActionProvider</code>.
 */
public interface ActionProvider<A extends Action> {

  ActionOptions options();

  Descriptors.ServiceDescriptor serviceDescriptor();

  ActionRouter<A> newRouter(ActionCreationContext context);

  Descriptors.FileDescriptor[] additionalDescriptors();

  default Optional<MessageCodec> alternativeCodec() {
    return Optional.empty();
  }
}
