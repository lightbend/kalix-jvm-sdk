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

  /** Command handler for "Increase". */
  public abstract Effect<Empty> increase(CounterDomain.CounterState currentState, CounterApi.IncreaseValue increaseValue);

  /** Command handler for "Decrease". */
  public abstract Effect<Empty> decrease(CounterDomain.CounterState currentState, CounterApi.DecreaseValue decreaseValue);

  /** Command handler for "Reset". */
  public abstract Effect<Empty> reset(CounterDomain.CounterState currentState, CounterApi.ResetValue resetValue);

  /** Command handler for "GetCurrentCounter". */
  public abstract Effect<CounterApi.CurrentCounter> getCurrentCounter(CounterDomain.CounterState currentState, CounterApi.GetCounter getCounter);

}