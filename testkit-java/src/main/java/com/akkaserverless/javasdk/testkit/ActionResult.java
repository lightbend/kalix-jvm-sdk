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
import com.akkaserverless.javasdk.ServiceCall;
import java.util.concurrent.CompletionStage;
import com.akkaserverless.javasdk.action.Action;

public interface ActionResult<T> {

  /** @return true if the call had an effect with a reply, false if not */
  boolean isReply();

  T getReply();

  /** @return true if the call was forwarded, false if not */
  boolean isForward();

  /** @return ServiceCall representing a call to a service */
  ServiceCall getForwardServiceCall();

  /** @return true if the call was async, false if not */
  boolean isAsync();

  /** @return a CompletionStage as a future ActionResult of an Action */
  CompletionStage<ActionResult<T>> getAsyncEffect();

  /** @return true if the call was an error, false if not */
  boolean isError();

  String getErrorDescription();

  /** @return true if the call had a noReply effect, false if not */
  boolean isNoReply();
}
