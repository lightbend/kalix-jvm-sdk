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

package com.akkaserverless.javasdk.testkit;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Represents the result of an EventSourcedEntity handling a command when run in through the
 * testkit.
 *
 * <p>Not for user extension, returned by the generated testkit.
 *
 * @param <R> The type of reply that is expected from invoking command handler
 */
public interface ActionResult<R> {

  /** @return true if the call had an effect with a reply, false if not */
  boolean isReply();

  /** @return true if the call was forwarded, false if not */
  boolean isForward();

  // TODO rewrite name. Async doesn't seem very descriptive. Are the rest sync?
  /** @return true if the call was async, false if not */
  boolean isAsync();

  /** @return true if the call was an error, false if not */
  boolean isError();

  /** @return true if the call had a noReply effect, false if not */
  boolean isNoReply();

  // boolean didEmitEffect();??? TODO review if useful

  /**
   * Look at the next effect and verify that it is of type E or fail if not or if there is no next
   * effect. If successful this consumes the effect, so that the next call to this method looks at
   * the next effect from here.
   *
   * @return The next effect if it is of type E, for additional assertions.
   */
  <E> E getEffectOfType(Class<E> expectedClass);
}
