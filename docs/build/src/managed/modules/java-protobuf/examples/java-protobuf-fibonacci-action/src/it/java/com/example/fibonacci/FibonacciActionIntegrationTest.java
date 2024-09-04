package com.example.fibonacci;

import kalix.javasdk.testkit.junit.jupiter.KalixTestKitExtension;
import com.example.Main;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.fail;

public class FibonacciActionIntegrationTest {
  /**
   * The test kit starts both the service container and the Kalix Runtime.
   */
  @RegisterExtension
  public static final KalixTestKitExtension testKit =
      new KalixTestKitExtension(Main.createKalix());

  private final Fibonacci client;

  public FibonacciActionIntegrationTest() {
    this.client = testKit.getGrpcClient(Fibonacci.class);
  }

  @Test
  public void calculateNextNumber() throws Exception {
    FibonacciApi.Number in = FibonacciApi.Number.newBuilder().setValue(5).build();
    FibonacciApi.Number response =
        client.nextNumber(in).toCompletableFuture().get(5, SECONDS);
    Assertions.assertEquals(8, response.getValue());
  }

  @Test
  public void wrongNumberReturnsError() throws Exception {
    FibonacciApi.Number in = FibonacciApi.Number.newBuilder().setValue(7).build();
    try {
      client.nextNumber(in).toCompletableFuture().get(5, SECONDS);
      fail("Should have failed");
    } catch (Exception ex) {
      Assertions.assertTrue(ex.getMessage().contains("Input number is not a Fibonacci number"));
    }

  }
}
