package com.example.fibonacci;

import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestKitResource;
import com.example.Main;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.fail;

public class FibonacciActionIntegrationTest {
  /**
   * The test kit starts both the service container and the Akka Serverless proxy.
   */
  @ClassRule
  public static final AkkaServerlessTestKitResource testkit =
      new AkkaServerlessTestKitResource(Main.createAkkaServerless());

  private final Fibonacci client;

  public FibonacciActionIntegrationTest() {
    this.client = testkit.getGrpcClient(Fibonacci.class, "Fibonacci");
  }

  @Test
  public void calculateNextNumber() throws Exception {
    FibonacciApi.Number in = FibonacciApi.Number.newBuilder().setValue(5).build();
    FibonacciApi.Number response =
        client.nextNumber(in).toCompletableFuture().get(5, SECONDS);
    Assert.assertEquals(8, response.getValue());
  }

  @Test
  public void wrongNumberReturnsError() throws Exception {
    FibonacciApi.Number in = FibonacciApi.Number.newBuilder().setValue(7).build();
    try {
      client.nextNumber(in).toCompletableFuture().get(5, SECONDS);
      fail("Should have failed");
    } catch (Exception ex) {
      Assert.assertTrue(ex.getMessage().contains("Input number is not a Fibonacci number"));
    }

  }
}
