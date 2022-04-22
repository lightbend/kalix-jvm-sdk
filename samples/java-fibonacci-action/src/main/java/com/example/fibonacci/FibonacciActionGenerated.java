package com.example.fibonacci;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import kalix.javasdk.action.ActionCreationContext;


// wrap the original FibonacciAction here so we can use it in the docs
public class FibonacciActionGenerated {

  /**
   * An action.
   */
  // tag::generated-action[]
  public class FibonacciAction extends AbstractFibonacciAction { // <1>

    public FibonacciAction(ActionCreationContext creationContext) {
    }

    /**
     * Handler for "NextNumber".
     */
    @Override
    public Effect<FibonacciApi.Number> nextNumber(FibonacciApi.Number number) { //<2>
      throw new RuntimeException("The command handler for `NextNumber` is not implemented, yet");
    }

    // end::generated-action[]
    public Source<Effect<FibonacciApi.Number>, NotUsed> nextNumbers(FibonacciApi.Number number) {
      throw new RuntimeException("The command handler for `NextNumber` is not implemented, yet");
    }
    public Effect<FibonacciApi.Number> nextNumberOfSum(Source<FibonacciApi.Number, NotUsed> numberSrc) {
      throw new RuntimeException("The command handler for `NextNumber` is not implemented, yet");
    }
    public Source<Effect<FibonacciApi.Number>, NotUsed> nextNumberOfEach(Source<FibonacciApi.Number, NotUsed> numberSrc) {
      throw new RuntimeException("The command handler for `NextNumber` is not implemented, yet");
    }
    // tag::generated-action[]
  }
  // end::generated-action[]
}
