package com.example.fibonacci;

import com.akkaserverless.javasdk.action.ActionCreationContext;


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

  }
  // end::generated-action[]
}
