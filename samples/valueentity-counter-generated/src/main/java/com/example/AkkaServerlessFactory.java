/* This code is managed by Akka Serverless tooling.
 * It will be re-generated to reflect any changes to your protobuf definitions.
 * DO NOT EDIT
 */

package com.example;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.example.CounterApi;
import com.example.actions.DoubleCounterAction;
import com.example.actions.DoubleCounterActionProvider;
import com.example.actions.DoubleCounterApi;
import com.example.domain.Counter;
import com.example.domain.CounterDomain;
import com.example.domain.CounterProvider;
import com.google.protobuf.EmptyProto;
import java.util.function.Function;

public final class AkkaServerlessFactory {

  public static AkkaServerless withComponents(
      Function<ValueEntityContext, Counter> createCounter,
      Function<ActionCreationContext, DoubleCounterAction> createDoubleCounterAction) {
    AkkaServerless akkaServerless = new AkkaServerless();
    return akkaServerless
      .register(CounterProvider.of(createCounter))
      .register(DoubleCounterActionProvider.of(createDoubleCounterAction));
  }
}