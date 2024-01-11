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

import kalix.javasdk.PassivationStrategy;
import kalix.javasdk.impl.valueentity.ValueEntityOptionsImpl;

import java.util.Set;

/** Root entity options for all value based entities. */
public interface ValueEntityOptions extends kalix.javasdk.EntityOptions {

  /**
   * @deprecated passivation strategy is ignored
   */
  @Override
  @Deprecated(since = "1.1.4", forRemoval = true)
  ValueEntityOptions withPassivationStrategy(PassivationStrategy strategy);

  @Override
  ValueEntityOptions withForwardHeaders(Set<String> headers);

  /**
   * Create a default entity option for a value based entity.
   *
   * @return the entity option
   */
  static ValueEntityOptions defaults() {
    return ValueEntityOptionsImpl.defaults();
  }
}
