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

package com.akkaserverless.tck.model.eventing;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.google.protobuf.Any;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

/** An action. */
public class LocalPersistenceSubscriberModelAction
    extends AbstractLocalPersistenceSubscriberModelAction {

  public LocalPersistenceSubscriberModelAction(ActionCreationContext creationContext) {}

  /** Handler for "ProcessEventOne". */
  @Override
  public Effect<LocalPersistenceEventing.Response> processEventOne(
      LocalPersistenceEventing.EventOne eventOne) {
    throw new RuntimeException("The command handler for `ProcessEventOne` is not implemented, yet");
  }

  /** Handler for "ProcessEventTwo". */
  @Override
  public Source<Effect<LocalPersistenceEventing.Response>, NotUsed> processEventTwo(
      LocalPersistenceEventing.EventTwo eventTwo) {
    throw new RuntimeException("The command handler for `ProcessEventTwo` is not implemented, yet");
  }
  /** Handler for "ProcessAnyEvent". */
  @Override
  public Effect<LocalPersistenceEventing.Response> processAnyEvent(Any any) {
    throw new RuntimeException("The command handler for `ProcessAnyEvent` is not implemented, yet");
  }
  /** Handler for "ProcessValueOne". */
  @Override
  public Effect<LocalPersistenceEventing.Response> processValueOne(
      LocalPersistenceEventing.ValueOne valueOne) {
    throw new RuntimeException("The command handler for `ProcessValueOne` is not implemented, yet");
  }

  /** Handler for "ProcessValueTwo". */
  @Override
  public Source<Effect<LocalPersistenceEventing.Response>, NotUsed> processValueTwo(
      LocalPersistenceEventing.ValueTwo valueTwo) {
    throw new RuntimeException("The command handler for `ProcessValueTwo` is not implemented, yet");
  }
  /** Handler for "ProcessAnyValue". */
  @Override
  public Effect<LocalPersistenceEventing.Response> processAnyValue(Any any) {
    throw new RuntimeException("The command handler for `ProcessAnyValue` is not implemented, yet");
  }
  /** Handler for "Effect". */
  @Override
  public Effect<LocalPersistenceEventing.Response> effect(
      LocalPersistenceEventing.EffectRequest effectRequest) {
    throw new RuntimeException("The command handler for `Effect` is not implemented, yet");
  }
}
