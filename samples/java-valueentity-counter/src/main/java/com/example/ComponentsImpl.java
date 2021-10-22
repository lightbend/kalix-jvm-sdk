/**
 *
 */
package com.example;

// FIXME generate and make accessible in all actions

import com.akkaserverless.javasdk.DeferredCall;
import com.akkaserverless.javasdk.DeferredCallRef;
import com.akkaserverless.javasdk.Metadata;
import com.akkaserverless.javasdk.action.ActionContext;
import com.example.actions.CounterStateSubscription;
import com.example.actions.DoubleCounter;
import com.google.protobuf.Any;
import com.google.protobuf.Empty;

import java.util.concurrent.CompletionStage;

/**
 * Not for user instantiation, access instance from Action#components()
 */
public final class ComponentsImpl implements Components {

  private final ActionContext context;

  public ComponentsImpl(ActionContext context) {
    this.context = context;
  }

  public Components.CounterCalls counter() {
    return new Components.CounterCalls() {
      @Override
      public DeferredCall<CounterApi.IncreaseValue, Empty> increase(CounterApi.IncreaseValue increase) {
        // FIXME make this all a single returned DeferredCall instance somehow rather than mixing two different ones
        DeferredCall<CounterApi.IncreaseValue, Empty> call =
            context.callFactory().<CounterApi.IncreaseValue, Empty>lookup(
                "com.example.CounterService",
                "Increase",
                CounterApi.IncreaseValue.class)
            .createCall(increase);

        return new DeferredCall<CounterApi.IncreaseValue, Empty>() {
          @Override
          public DeferredCallRef<CounterApi.IncreaseValue, Empty> ref() {
            // note that we now _know_ the return type is ok even if the lookup doesn't prove that
            return call.ref();
          }

          @Override
          public Any message() {
            return call.message();
          }

          @Override
          public Metadata metadata() {
            return call.metadata();
          }

          @Override
          public CompletionStage<Empty> execute() {
            // FIXME how to actually connect to the service itself (localhost - does it even work, service name?)
            return context.getGrpcClient(CounterService.class, "localhost").increase(increase);
          }
        };
      }

      @Override
      public DeferredCall<CounterApi.DecreaseValue, Empty> decrease(CounterApi.DecreaseValue decreaseValue) {
        throw new UnsupportedOperationException("Placeholder call for now");
      }

      @Override
      public DeferredCall<CounterApi.GetCounter, CounterApi.CurrentCounter> getCurrentCounter(CounterApi.GetCounter getCounter) {
        throw new UnsupportedOperationException("Placeholder call for now");
      }

      @Override
      public DeferredCall<CounterApi.ResetValue, Empty> reset(CounterApi.ResetValue resetValue) {
        throw new UnsupportedOperationException("Placeholder call for now");
      }
    };
  }

  @Override
  public Components.DoubleCounterActionCalls doubleCounterAction() {
    throw new UnsupportedOperationException("Placeholder call for now");
  }
  @Override
  public CounterStateSubscriptionActionCalls counterStateSubscriptionAction() {
    throw new UnsupportedOperationException("Placeholder call for now");
  }
}