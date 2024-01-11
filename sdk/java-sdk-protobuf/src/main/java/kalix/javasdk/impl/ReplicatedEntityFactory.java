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

package kalix.javasdk.impl;

import kalix.javasdk.impl.replicatedentity.ReplicatedEntityRouter;
import kalix.javasdk.replicatedentity.ReplicatedEntity;
import kalix.javasdk.replicatedentity.ReplicatedEntityContext;

/**
 * Low level interface for handling commands on a replicated entity.
 *
 * <p>Generally, this should not be needed, instead, a class extending a generated abstract {@link
 * ReplicatedEntity} should be used.
 */
public interface ReplicatedEntityFactory {
  /**
   * Create an entity handler for the given context.
   *
   * @param context The context.
   * @return The handler for the given context.
   */
  ReplicatedEntityRouter<?, ?> create(ReplicatedEntityContext context);
}
