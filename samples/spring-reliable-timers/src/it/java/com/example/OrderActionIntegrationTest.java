package com.example;

import com.example.domain.Order;
import com.example.domain.OrderRequest;
import com.example.domain.OrderStatus;
import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class OrderActionIntegrationTest extends KalixIntegrationTestKitSupport {

  private Duration timeout = Duration.of(20, SECONDS);


  @Autowired
  private WebClient webClient;

  @Test
  public void placeOrder() {

    var orderReq = new OrderRequest("nice swag tshirt", 10);

    String orderId =
        webClient.post()
            .uri("/orders/place")
            .bodyValue(orderReq)
            .retrieve()
            .bodyToMono(Order.class)
            .block(timeout)
            .id();

    Assertions.assertNotNull(orderId);
    Assertions.assertFalse(orderId.isEmpty());

    await()
        .ignoreExceptions()
        .atMost(Duration.ofSeconds(20))
        .until(
            () ->
                webClient.get()
                    .uri("/order/" + orderId)
                    .retrieve()
                    .bodyToMono(OrderStatus.class)
                    .block(timeout),
            s -> s.quantity() == 10 && s.item().equals("nice swag tshirt"));

    webClient.post()
        .uri("/orders/confirm/" + orderId)
        .retrieve()
        .bodyToMono(String.class)
        .block(timeout);

    await()
        .ignoreExceptions()
        .atMost(Duration.ofSeconds(20))
        .until(
            () ->
                webClient.get()
                    .uri("/order/" + orderId)
                    .retrieve()
                    .bodyToMono(OrderStatus.class)
                    .block(timeout),
            OrderStatus::confirmed);

  }

  @Test
  public void expiredOrder() {

    var orderReq = new OrderRequest("nice swag tshirt", 20);

    String orderId =
        webClient.post()
            .uri("/orders/place")
            .bodyValue(orderReq)
            .retrieve()
            .bodyToMono(Order.class)
            .block(timeout)
            .id();


    Assertions.assertNotNull(orderId);
    Assertions.assertFalse(orderId.isEmpty());

    // After the default timeout, status changed to not placed as order is reverted
    await()
        .ignoreExceptions()
        .atMost(Duration.ofSeconds(20))
        .until(
            () ->
                webClient.get()
                    .uri("/order/" + orderId)
                    .exchangeToMono((resp) -> Mono.just(resp.statusCode().value()))
                    .block(timeout),
            s -> s == 404);
  }

}
