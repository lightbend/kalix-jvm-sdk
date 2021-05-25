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

package com.akkaserverless.javasdk.replicatedentity;

/**
 * Low level interface for handling commands for replicated entities.
 *
 * <p>Generally, this should not be used, rather, a {@link ReplicatedEntity} annotated class should
 * be used.
 */
public interface ReplicatedEntityHandlerFactory {
  /**
   * Create a Replicated Entity handler for the given context.
   *
   * <p>This will be invoked each time a new replicated entity stream from the proxy is established,
   * for handling commands for a single Replicated Entity.
   *
   * @param context The creation context.
   * @return The handler to handle commands.
   */
  ReplicatedEntityHandler create(ReplicatedEntityCreationContext context);
}
