package com.example.shoppingcart;

import com.example.shoppingcart.ShoppingCartApi.AddLineItem;
import com.example.shoppingcart.ShoppingCartController.NewCart;
import com.example.shoppingcart.ShoppingCartController.NewCartCreated;
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
    var mockRegistry = new TestKitMockRegistry(Map.of(ShoppingCartService.class, shoppingCartService));

    var testKit = ShoppingCartActionImplTestKit.of(ShoppingCartActionImpl::new, mockRegistry);
    var result = testKit.initializeCart(NewCart.newBuilder().build());

    var asyncResult = (CompletableFuture<ActionResult<NewCartCreated>>) result.getAsyncResult();
    assertFalse(asyncResult.get(1, TimeUnit.SECONDS).getReply().getCartId().isEmpty());
  }

  @Test
  public void prePopulatedCartTest() throws ExecutionException, InterruptedException, TimeoutException {
    when(shoppingCartService.create(notNull()))
            .thenReturn(CompletableFuture.completedFuture(Empty.getDefaultInstance()));
    when(shoppingCartService.addItem(any()))
            .thenReturn(CompletableFuture.completedFuture(Empty.getDefaultInstance()));
    var mockRegistry = new TestKitMockRegistry(Map.of(ShoppingCartService.class, shoppingCartService));

    var service = ShoppingCartActionImplTestKit.of(ShoppingCartActionImpl::new, mockRegistry);
    var result = service.createPrePopulated(NewCart.getDefaultInstance()).getAsyncResult();
    var reply = ((CompletableFuture<ActionResult<NewCartCreated>>) result)
            .get(1, TimeUnit.SECONDS)
            .getReply();

    ArgumentCaptor<AddLineItem> lineItem = ArgumentCaptor.forClass(AddLineItem.class);
    verify(shoppingCartService).addItem(lineItem.capture());
    assertEquals("eggplant", lineItem.getValue().getName());
    assertEquals(reply.getCartId(), lineItem.getValue().getCartId());
  }

}
