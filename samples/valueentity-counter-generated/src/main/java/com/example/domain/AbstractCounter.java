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

/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */
package com.example.domain;

import com.akkaserverless.javasdk.valueentity.ValueEntityBase;
import com.example.CounterApi;
import com.google.protobuf.Empty;

/** A value entity. */
public abstract class AbstractCounter extends ValueEntityBase<CounterDomain.CounterState> {

  public abstract Effect<Empty> increase(CounterDomain.CounterState currentState, CounterApi.IncreaseValue increaseValue);
  public abstract Effect<Empty> decrease(CounterDomain.CounterState currentState, CounterApi.DecreaseValue decreaseValue);
  public abstract Effect<Empty> reset(CounterDomain.CounterState currentState, CounterApi.ResetValue resetValue);
  public abstract Effect<CounterApi.CurrentCounter> getCurrentCounter(CounterDomain.CounterState currentState, CounterApi.GetCounter getCounter);

}
