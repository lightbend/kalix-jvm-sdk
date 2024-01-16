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

package kalix.javasdk.valueentity;

import kalix.javasdk.Kalix;
import kalix.javasdk.impl.MessageCodec;
import kalix.javasdk.impl.valueentity.ValueEntityRouter;
import com.google.protobuf.Descriptors;

import java.util.Optional;

/**
 * Register a value based entity in {@link Kalix} using a <code>
 * ValueEntityProvider</code>. The concrete <code>ValueEntityProvider</code> is generated for the
 * specific entities defined in Protobuf, for example <code>CustomerEntityProvider</code>.
 */
public interface ValueEntityProvider<S, E extends ValueEntity<S>> {

  ValueEntityOptions options();

  Descriptors.ServiceDescriptor serviceDescriptor();

  String typeId();

  ValueEntityRouter<S, E> newRouter(ValueEntityContext context);

  Descriptors.FileDescriptor[] additionalDescriptors();

  default Optional<MessageCodec> alternativeCodec() {
    return Optional.empty();
  }
}
