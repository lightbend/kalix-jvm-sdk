package com.example.shoppingcart;

import akka.stream.javadsl.Source;
import com.example.shoppingcart.domain.ShoppingCart;
import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ActionResult;
import com.example.shoppingcart.ShoppingCartActionImpl;
import com.example.shoppingcart.ShoppingCartActionImplTestKit;
import com.example.shoppingcart.ShoppingCartController;
import kalix.javasdk.testkit.impl.TestKitMockRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.when;
import scala.collection.JavaConverters.AsScala;

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
  public void initializeCartTest() {
    when(shoppingCartService.create(notNull()))
            .thenReturn(new CompletableFuture<>());

    TestKitMockRegistry mockRegistry = new TestKitMockRegistry(Map.of(ShoppingCartService.class, shoppingCartService));
    // TODO inject mock registry

    ShoppingCartActionImplTestKit testKit = ShoppingCartActionImplTestKit.of(ShoppingCartActionImpl::new);
    ActionResult<ShoppingCartController.NewCartCreated> result = testKit.initializeCart(ShoppingCartController.NewCart.newBuilder().build());

  }



}
