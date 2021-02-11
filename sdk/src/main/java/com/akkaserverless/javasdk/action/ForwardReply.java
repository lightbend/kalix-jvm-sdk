/*
 * Copyright 2019 Lightbend Inc.
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

package com.akkaserverless.javasdk.action;

import com.akkaserverless.javasdk.ServiceCall;

import java.util.Collection;

/** A forward reply. */
public interface ForwardReply<T> extends ActionReply<T> {

  /**
   * The service call that is being forwarded to.
   *
   * @return The service call.
   */
  ServiceCall serviceCall();

  ForwardReply<T> withEffects(Collection<Effect> effects);

  ForwardReply<T> withEffects(Effect... effects);
}
