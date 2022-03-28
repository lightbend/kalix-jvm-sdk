package org.example.valueentity;

import kalix.javasdk.testkit.ValueEntityResult;
import kalix.javasdk.valueentity.ValueEntity;
import com.google.protobuf.Empty;
import org.example.valueentity.domain.CounterDomain;
import org.junit.Test;

import static org.junit.Assert.*;

// This class was initially generated based on the .proto definition by Akka Serverless tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class CounterServiceEntityTest {

  @Test
  public void exampleTest() {
    CounterServiceEntityTestKit testKit = CounterServiceEntityTestKit.of(CounterServiceEntity::new);
    // use the testkit to execute a command
    // of events emitted, or a final updated state:
    // ValueEntityResult<SomeResponse> result = testKit.someOperation(SomeRequest);
    // verify the response
    // SomeResponse actualResponse = result.getReply();
    // assertEquals(expectedResponse, actualResponse);
    // verify the final state after the command
    // assertEquals(expectedState, testKit.getState());
  }

  @Test
  public void increaseTest() {
    CounterServiceEntityTestKit testKit = CounterServiceEntityTestKit.of(CounterServiceEntity::new);
    // ValueEntityResult<Empty> result = testKit.increase(IncreaseValue.newBuilder()...build());
  }


  @Test
  public void decreaseTest() {
    CounterServiceEntityTestKit testKit = CounterServiceEntityTestKit.of(CounterServiceEntity::new);
    // ValueEntityResult<Empty> result = testKit.decrease(DecreaseValue.newBuilder()...build());
  }

}
