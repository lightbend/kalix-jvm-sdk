package com.example.actions;

import com.example.CounterApi.*;
import com.example.CounterService;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.MockRegistry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class ExternalCounterActionTest {

  @Mock
  private CounterService counterService;

  private AutoCloseable closeable;

  @BeforeEach
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @AfterEach
  public void tearDown() throws Exception {
    closeable.close();
  }

  @Test
  public void increaseTest() throws ExecutionException, InterruptedException, TimeoutException {
    when(counterService.increase(notNull()))
            .thenReturn(CompletableFuture.completedFuture(Empty.getDefaultInstance()));
    var mockRegistry = MockRegistry.create().withMock(CounterService.class, counterService);

    var service = ExternalCounterActionTestKit.of(ExternalCounterAction::new, mockRegistry);
    var result = service.increase(IncreaseValue.getDefaultInstance());

    var asyncResult = (CompletableFuture<ActionResult<Empty>>) result.getAsyncResult();
    assertNotNull(asyncResult.get(1, TimeUnit.SECONDS).getReply());
  }

}
