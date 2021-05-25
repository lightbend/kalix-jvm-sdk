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

import com.akkaserverless.javasdk.EntityContext;

import java.util.Optional;

/** Root context for all Replicated Entity contexts. */
public interface ReplicatedEntityContext extends EntityContext {
  /**
   * The current Replicated Data object, if it's been created.
   *
   * @param dataClass The type of the Replicated Data that is expected.
   * @return The current Replicated Data, or empty if none has been created yet.
   * @throws IllegalStateException If the current Replicated Data does not match the passed in
   *     <code>dataClass</code> type.
   */
  <T extends ReplicatedData> Optional<T> state(Class<T> dataClass) throws IllegalStateException;

  /**
   * Get the current write consistency setting for replication of the replicated entity state.
   *
   * @return the current write consistency
   */
  WriteConsistency getWriteConsistency();

  /**
   * Set the write consistency setting for replication of the replicated entity state.
   *
   * @param writeConsistency the new write consistency to use
   */
  void setWriteConsistency(WriteConsistency writeConsistency);
}
