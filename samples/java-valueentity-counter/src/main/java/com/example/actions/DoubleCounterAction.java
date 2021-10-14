/* This code was generated by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.actions;

import com.akkaserverless.javasdk.ServiceCallRef;
import com.akkaserverless.javasdk.SideEffect;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.example.Components;
import com.example.CounterApi;
import com.google.protobuf.Empty;

import java.util.concurrent.CompletionStage;

// tag::controller-forward[]
// tag::controller-side-effect[]
/**
 * An action.
 */
public class DoubleCounterAction extends AbstractDoubleCounterAction {

  private final ServiceCallRef<CounterApi.IncreaseValue> increaseCallRef;

  public DoubleCounterAction(ActionCreationContext creationContext) {
    this.increaseCallRef =
        creationContext.serviceCallFactory() // <1>
            .lookup(
                "com.example.CounterService",
                "Increase",
                CounterApi.IncreaseValue.class);
  }

  // FIXME generated method in AbstractDoubleCounterAction
  protected final Components components() {
    return null; // new ComponentsImpl(actionContext())
  }
// end::controller-side-effect[]
// tag::controller-side-effect[]
  
  // Handler for "Increase" not shown in this snippet

// end::controller-side-effect[]

  /**
   * Handler for "Increase".
   */
  @Override
  public Effect<Empty> increase(CounterApi.IncreaseValue increaseValue) {
    int doubled = increaseValue.getValue() * 2;
    CounterApi.IncreaseValue increaseValueDoubled =
        increaseValue.toBuilder().setValue(doubled).build(); // <2>

    return effects()
            .forward(increaseCallRef.createCall(increaseValueDoubled)); // <3>
  }

  // end::controller-forward[]
  // tag::controller-side-effect[]
  /**
   * Handler for "IncreaseWithSideEffect".
   */
  @Override 
  public Effect<Empty> increaseWithSideEffect(CounterApi.IncreaseValue increaseValue) {
    int doubled = increaseValue.getValue() * 2;
    CounterApi.IncreaseValue increaseValueDoubled =
        increaseValue.toBuilder().setValue(doubled).build(); // <2>

    return effects()
            .reply(Empty.getDefaultInstance()) // <3>
            .addSideEffect( // <4>
                SideEffect.of(increaseCallRef.createCall(increaseValueDoubled)));
  }
  // end::controller-side-effect[]

  // almost like forward, we could potentially detect no map and turn this into a forward effect
  public Effect<Empty> forwardWithGrpcApi(CounterApi.IncreaseValue increaseValue) {
    int doubled = increaseValue.getValue() * 2;
    CounterApi.IncreaseValue increaseValueDoubled =
        increaseValue.toBuilder().setValue(doubled).build();
    return effects().asyncReply(components().counter().increase(increaseValueDoubled));
  }

  // regular async sequence of operations
  public Effect<CounterApi.CurrentCounter> sequentialComposition(CounterApi.IncreaseValue increaseValue) {
    int doubled = increaseValue.getValue() * 2;
    CounterApi.IncreaseValue increaseValueDoubled =
        increaseValue.toBuilder().setValue(doubled).build();
    CompletionStage<CounterApi.CurrentCounter> increaseAndValueAfter =
        components().counter().increase(increaseValueDoubled)
        .thenCompose(empty ->
            components().counter().getCurrentCounter(
                CounterApi.GetCounter.newBuilder().setCounterId(increaseValue.getCounterId()).build())
        );
    return effects().asyncReply(increaseAndValueAfter);
  }

  // Maybe this is not something we should show in docs, happens in parallel (right now)
  public Effect<CounterApi.CurrentCounter> sumOfMy3FavouriteCounterValues(Empty empty) {
    CompletionStage<CounterApi.CurrentCounter> counter1 = components().counter().getCurrentCounter(CounterApi.GetCounter.newBuilder().setCounterId("counter-1").build());
    CompletionStage<CounterApi.CurrentCounter> counter2 = components().counter().getCurrentCounter(CounterApi.GetCounter.newBuilder().setCounterId("counter-2").build());
    CompletionStage<CounterApi.CurrentCounter> counter3 = components().counter().getCurrentCounter(CounterApi.GetCounter.newBuilder().setCounterId("counter-3").build());

    CompletionStage<CounterApi.CurrentCounter> sumOfAllThree = counter1.thenCombine(counter2, (currentCounter1, currentCounter2) ->
        currentCounter1.getValue() + currentCounter2.getValue()
    ).thenCombine(counter3, (sumOf1And2, currentCounter3) ->
        CounterApi.CurrentCounter.newBuilder().setValue(sumOf1And2 + currentCounter3.getValue()).build()
    );

    return effects().asyncReply(sumOfAllThree);
  }
  // tag::controller-side-effect[]
  // tag::controller-forward[]
}
// end::controller-forward[]
// end::controller-side-effect[]