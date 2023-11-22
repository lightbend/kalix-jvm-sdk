package com.example;

import com.example.api.ShoppingCartDTO;
import com.example.api.ShoppingCartDTO.LineItemDTO;
import com.example.domain.ShoppingCart;
import kalix.spring.testkit.KalixIntegrationTestKitSupport;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * This is a skeleton for implmenting integration tests for a Kalix application built with the Java SDK.
 * <p>
 * This test will initiate a Kalix Runtime using testcontainers and therefore it's required to have Docker installed
 * on your machine. This test will also start your Spring Boot application.
 * <p>
 * Since this is an integration tests, it interacts with the application using a WebClient
 * (already configured and provided automatically through injection).
 */
@SpringBootTest(classes = Main.class)
public class IntegrationTest extends KalixIntegrationTestKitSupport {

  @Autowired
  private WebClient webClient;

  private Duration timeout = Duration.of(5, SECONDS);

  ShoppingCartDTO getCart(String cartId) {
    return
      webClient.get()
        .uri("/cart/" + cartId)
        .retrieve()
        .bodyToMono(ShoppingCartDTO.class)
        .block(timeout);
  }

  void addItem(String cartId, String productId, String name, int quantity) throws Exception {
    webClient.post()
      .uri("/cart/" + cartId + "/items/add")
      .bodyValue(new LineItemDTO(productId, name, quantity))
      .retrieve()
      .bodyToMono(ShoppingCartDTO.class)
      .block(timeout);
  }

  void removeItem(String cartId, String productId) throws Exception {
    webClient.post()
      .uri("/cart/" + cartId + "/items/" + productId + "/remove")
      .retrieve()
      .bodyToMono(ShoppingCartDTO.class)
      .block(timeout);
  }

  void removeCart(String cartId, String userRole) throws Exception {
    webClient.delete()
      .uri("/carts/" + cartId)
      .header("UserRole", userRole)
      .retrieve()
      .bodyToMono(String.class)
      .block(timeout);
  }

  LineItemDTO item(String productId, String name, int quantity) {
    return new LineItemDTO(productId, name, quantity);
  }

  ShoppingCartDTO initializeCart() throws Exception {
    return
      webClient.post()
        .uri("/carts/create")
        .retrieve()
        .bodyToMono(ShoppingCartDTO.class)
        .block(timeout);
  }

  String createPrePopulated() throws Exception {
    return
      webClient.post()
        .uri("/carts/prepopulated")
        .retrieve()
        .bodyToMono(String.class)
        .block(timeout);
  }

  ShoppingCartDTO verifiedAddItem(String cartId, LineItemDTO in) throws Exception {
    return
      webClient.post()
        .uri("/carts/" + cartId + "/items/add")
        .bodyValue(in)
        .retrieve()
        .bodyToMono(ShoppingCartDTO.class)
        .block(timeout);
  }

  @Test
  public void emptyCartByDefault() throws Exception {
    assertEquals(0, getCart("user1").items().size(), "shopping cart should be empty");
  }

  @Test
  public void addItemsToCart() throws Exception {
    addItem("cart2", "a", "Apple", 1);
    addItem("cart2", "b", "Banana", 2);
    addItem("cart2", "c", "Cantaloupe", 3);
    var cart = getCart("cart2");
    assertEquals(3, cart.items().size(), "shopping cart should have 3 items");
    assertEquals(
      List.of(item("a", "Apple", 1), item("b", "Banana", 2), item("c", "Cantaloupe", 3)),
      cart.items(),
      "shopping cart should have expected items");
  }

  @Test
  public void removeItemsFromCart() throws Exception {
    addItem("cart3", "a", "Apple", 1);
    addItem("cart3", "b", "Banana", 2);
    var cart1 = getCart("cart3");
    assertEquals(2, cart1.items().size(), "shopping cart should have 2 items");
    assertEquals(
      cart1.items(),
      List.of(item("a", "Apple", 1), item("b", "Banana", 2)),
      "shopping cart should have expected items");
    removeItem("cart3", "a");
    var cart2 = getCart("cart3");
    assertEquals(1, cart2.items().size(), "shopping cart should have 1 item");
    assertEquals(
      cart2.items(),
      List.of(item("b", "Banana", 2)),
      "shopping cart should have expected items");
  }

  @Test
  public void removeCart() throws Exception {
    addItem("cart4", "a", "Apple", 42);
    var cart1 = getCart("cart4");
    assertEquals(1, cart1.items().size(), "shopping cart should have 1 item");
    assertEquals(
      cart1.items(),
      List.of(item("a", "Apple", 42)),
      "shopping cart should have expected items");
    removeCart("cart4", "Admin");
    assertEquals(0, getCart("cart4").items().size(), "shopping cart should be empty");
  }

  @Test
  public void createNewPrePopulatedCart() throws Exception {
    String cartId = createPrePopulated();
    var cart = getCart(cartId.substring(1, cartId.length() - 1)); // removing quotes
    assertEquals(1, cart.items().size());
  }

  @Test
  public void verifiedAddItem() throws Exception {
    final String cartId = "carrot-cart";
    assertThrows(Exception.class, () ->
      verifiedAddItem(
        cartId,
        new LineItemDTO("c", "Carrot", 4)
      )
    );
    verifiedAddItem(
      cartId,
      new LineItemDTO("b", "Banana", 1));
    var cart = getCart(cartId);
    assertEquals(1, cart.items().size());
  }
}