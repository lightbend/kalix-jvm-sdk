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

package com.akkaserverless.replicatedentity;

import akka.annotation.InternalApi;

/** Root interface for all data entries in Replicated Entities. */
public interface ReplicatedData {

  /**
   * INTERNAL API This method is used internally to control if instances have been created by
   * internal factory methods. Specially needed for the Scala SDK that wraps the Java
   * implementation. In which case, _internal() will return the Java internal delegate. The Java
   * implementations will return {@code this}.
   */
  @InternalApi
  ReplicatedData _internal();
}
