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

import com.akkaserverless.javasdk.EntityOptions;
import com.akkaserverless.javasdk.PassivationStrategy;
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityOptionsImpl;
import scala.collection.immutable.Set;

import java.util.Collections;

/** Root entity options for all Replicated Entities. */
public interface ReplicatedEntityOptions extends EntityOptions {

  ReplicatedEntityOptions withPassivationStrategy(PassivationStrategy strategy);

  /**
   * Create default Replicated Entity options.
   *
   * @return the entity options
   */
  static ReplicatedEntityOptions defaults() {
    return new ReplicatedEntityOptionsImpl(
        PassivationStrategy.defaultTimeout(), Collections.emptySet());
  }
}
