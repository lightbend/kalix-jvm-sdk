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

/**
 * Represents the result of an ValueEntity handling a command when run in through the testkit.
 *
 * @param <R> The type of reply that is expected from invoking command handler
 */
// FIXME a way to inspect other effects than reply?
public final class ValueEntityResult<R> {

  private final R reply;

  /**
   * INTERNAL API
   *
   * <p>Constructed by the generated code, not intended for calls from user code.
   */
  public ValueEntityResult(R reply) {
    this.reply = reply;
  }

  /** The reply object from the handler if there was one. */
  public R getReply() {
    return reply;
  }
}
