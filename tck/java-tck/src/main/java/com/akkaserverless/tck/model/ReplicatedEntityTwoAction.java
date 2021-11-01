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

package com.akkaserverless.tck.model;

import com.akkaserverless.javasdk.action.ActionCreationContext;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An action. */
public class ReplicatedEntityTwoAction extends AbstractReplicatedEntityTwoAction {

  public ReplicatedEntityTwoAction(ActionCreationContext creationContext) {}

  /** Handler for "Call". */
  @Override
  public Effect<ReplicatedEntity.Response> call(ReplicatedEntity.Request request) {
    throw new RuntimeException("The command handler for `Call` is not implemented, yet");
  }
}
