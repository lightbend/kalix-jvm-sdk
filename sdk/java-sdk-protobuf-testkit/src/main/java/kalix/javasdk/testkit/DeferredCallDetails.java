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

package kalix.javasdk.testkit;

import kalix.javasdk.Metadata;

public interface DeferredCallDetails<I, O> {
  /** @return The forwarded message */
  I getMessage();

  /** @return Any metadata attached to the call */
  Metadata getMetadata();

  /** @return The name of the service being called */
  String getServiceName();

  /** @return The method name being called */
  String getMethodName();
}
