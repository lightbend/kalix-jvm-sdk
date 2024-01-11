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

package kalix.javasdk.replicatedentity;

import kalix.javasdk.EntityOptions;
import kalix.javasdk.PassivationStrategy;
import kalix.javasdk.impl.replicatedentity.ReplicatedEntityOptionsImpl;

import java.util.Collections;

/** Root entity options for all Replicated Entities. */
public interface ReplicatedEntityOptions extends EntityOptions {

  ReplicatedEntityOptions withPassivationStrategy(PassivationStrategy strategy);

  /**
   * Get the current write consistency setting for replication of the replicated entity state.
   *
   * @return the write consistency setting for a replicated entity
   */
  WriteConsistency writeConsistency();

  /**
   * Set the write consistency setting for replication of the replicated entity state.
   *
   * @param writeConsistency write consistency to use
   * @returns new replicated entity options with write consistency setting
   */
  ReplicatedEntityOptions withWriteConsistency(WriteConsistency writeConsistency);

  /**
   * Create default Replicated Entity options.
   *
   * @return the entity options
   */
  static ReplicatedEntityOptions defaults() {
    return new ReplicatedEntityOptionsImpl(
        PassivationStrategy.defaultTimeout(), Collections.emptySet(), WriteConsistency.LOCAL);
  }
}
