package com.example.fibonacci;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import kalix.javasdk.testkit.ActionResult;
import com.example.fibonacci.FibonacciAction;
import com.example.fibonacci.FibonacciActionTestKit;
import com.example.fibonacci.FibonacciApi;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// tag::class[]
public class FibonacciActionTest {

  @Test
  public void nextNumberTest() {
    FibonacciActionTestKit testKit = FibonacciActionTestKit.of(FibonacciAction::new); // <1>
    ActionResult<FibonacciApi.Number> result = testKit.nextNumber(FibonacciApi.Number.newBuilder().setValue(5).build()); // <2>
    assertEquals(8, result.getReply().getValue()); // <3>
  }

  // end::class[]
  @Test
  public void nextNumbersTest() throws Exception {
    FibonacciActionTestKit testKit = FibonacciActionTestKit.of(FibonacciAction::new);
    ActorSystem system = ActorSystem.create("fibonacci1"); // needed to run stream
    try {
      Source<ActionResult<FibonacciApi.Number>, NotUsed> result =
          testKit.nextNumbers(FibonacciApi.Number.newBuilder().setValue(3).build());

      List<ActionResult<FibonacciApi.Number>> resultEffects =
          result.take(2).runWith(Sink.seq(), system)
              .toCompletableFuture().get(3, TimeUnit.SECONDS);

      assertEquals(5, resultEffects.get(0).getReply().getValue());
      assertEquals(8, resultEffects.get(1).getReply().getValue());
    } finally {
      system.terminate();
    }
  }

  // Not possible until testkit provides materializer https://github.com/lightbend/kalix-jvm-sdk/issues/495
  /*
  @Test
  public void nextNumberOfSumTest() {
    FibonacciActionTestKit testKit = FibonacciActionTestKit.of(FibonacciAction::new);

    ActionResult<FibonacciApi.Number> result = testKit.nextNumberOfSum(Source.single(FibonacciApi.Number.newBuilder().setValue(5).build()));
    assertEquals(8, result.getReply().getValue());
  }
   */

  @Test
  public void nextNumberOfEachTest() throws Exception {
    FibonacciActionTestKit testKit = FibonacciActionTestKit.of(FibonacciAction::new);
    ActorSystem system = ActorSystem.create("fibonacci1"); // needed to run stream
    try {
      Source<ActionResult<FibonacciApi.Number>, akka.NotUsed> result = testKit.nextNumberOfEach(Source.from(Arrays.asList(
          FibonacciApi.Number.newBuilder().setValue(3).build(),
          FibonacciApi.Number.newBuilder().setValue(5).build())));
      List<ActionResult<FibonacciApi.Number>> actionResults = result.runWith(Sink.seq(), system)
          .toCompletableFuture().get(3, TimeUnit.SECONDS);
      assertEquals(5, actionResults.get(0).getReply().getValue());
      assertEquals(8, actionResults.get(1).getReply().getValue());
    } finally {
      system.terminate();
    }
  }

  // tag::class[]
}
// end::class[]