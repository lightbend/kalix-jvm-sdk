package com.example.domain;

import java.math.BigInteger;

import com.example.FactorialApi;
import com.example.FactorialApi.FactorialResponse;

import kalix.javasdk.valueentity.ValueEntityContext;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the Value Entity Service described in your com/example/factorial_api.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class Factorial extends AbstractFactorial {
  @SuppressWarnings("unused")
  private final String entityId;

  public Factorial(ValueEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public FactorialDomain.FactorialState emptyState() {
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state");
  }

  @Override
  public Effect<FactorialApi.FactorialResponse> getFactorial(FactorialDomain.FactorialState currentState, FactorialApi.FactorialRequest factorialRequest) {
    //add span
    FactorialDomain.FactorialState state = currentState;
    if (!currentState.isInitialized()) {
      BigInteger resultBigInteger = factorial(Integer.parseInt(factorialRequest.getFactorialId()));
      state = currentState.toBuilder().setValue(resultBigInteger.toString()).build();
    }
    //add span
    return effects().updateState(state).thenReply(FactorialResponse.newBuilder()
    .setFactorial(state.getValue())
    .build());
  }

  public static BigInteger factorial(int n) {
    if (n < 0) {
      throw new IllegalArgumentException("Factorial is not defined for negative numbers");
    }

    BigInteger result = BigInteger.ONE;
    for (int i = 2; i <= n; i++) {
      result = result.multiply(BigInteger.valueOf(i));
    }
    return result;
  }
}
