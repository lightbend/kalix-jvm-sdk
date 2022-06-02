package com.example.shoppingcart;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ActionResult;
import kalix.javasdk.testkit.impl.TestKitMockRegistry;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ShoppingCartActionImplTest {

  @Mock
  private ShoppingCartService shoppingCartService;

  private AutoCloseable closeable;

  @Before
  public void setUp() {
    closeable = MockitoAnnotations.openMocks(this);
  }

  @Before
  public void tearDown() throws Exception {
    closeable.close();
  }

  @Test
  public void initializeCartTest() throws ExecutionException, InterruptedException, TimeoutException {
    when(shoppingCartService.create(notNull()))
            .thenReturn(CompletableFuture.completedFuture(Empty.getDefaultInstance()));

    TestKitMockRegistry mockRegistry = new TestKitMockRegistry(Map.of(ShoppingCartService.class, shoppingCartService));

    ShoppingCartActionImplTestKit testKit = ShoppingCartActionImplTestKit.of(ShoppingCartActionImpl::new, mockRegistry);
    ActionResult<ShoppingCartController.NewCartCreated> result = testKit.initializeCart(ShoppingCartController.NewCart.newBuilder().build());

    CompletableFuture<ActionResult<ShoppingCartController.NewCartCreated>> asyncResult = (CompletableFuture<ActionResult<ShoppingCartController.NewCartCreated>>) result.getAsyncResult();
    assertFalse(asyncResult.get(1, TimeUnit.SECONDS).getReply().getCartId().isEmpty());
  }

  @Test
  public void prePopulatedCartTest() throws ExecutionException, InterruptedException, TimeoutException {
    when(shoppingCartService.create(notNull()))
            .thenReturn(CompletableFuture.completedFuture(Empty.getDefaultInstance()));
    when(shoppingCartService.addItem(any()))
            .thenReturn(CompletableFuture.completedFuture(Empty.getDefaultInstance()));
    TestKitMockRegistry mockRegistry = new TestKitMockRegistry(Map.of(ShoppingCartService.class, shoppingCartService));

    ShoppingCartActionImplTestKit testKit = ShoppingCartActionImplTestKit.of(ShoppingCartActionImpl::new, mockRegistry);
    ActionResult<ShoppingCartController.NewCartCreated> result = testKit.createPrePopulated(ShoppingCartController.NewCart.newBuilder().build());
    ShoppingCartController.NewCartCreated reply = ((CompletableFuture<ActionResult<ShoppingCartController.NewCartCreated>>) result.getAsyncResult()).get(1, TimeUnit.SECONDS).getReply();

    ArgumentCaptor<ShoppingCartApi.AddLineItem> lineItem = ArgumentCaptor.forClass(ShoppingCartApi.AddLineItem.class);
    verify(shoppingCartService).addItem(lineItem.capture());
    assertEquals("eggplant", lineItem.getValue().getName());
    assertEquals(reply.getCartId(), lineItem.getValue().getCartId());
  }

}
