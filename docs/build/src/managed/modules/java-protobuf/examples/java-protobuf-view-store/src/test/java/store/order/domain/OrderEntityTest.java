package store.order.domain;

import com.google.protobuf.Empty;
import kalix.javasdk.testkit.ValueEntityResult;
import org.junit.jupiter.api.Test;
import store.order.api.OrderApi;

import static org.junit.jupiter.api.Assertions.*;

public class OrderEntityTest {

  @Test
  public void createAndGetTest() {
    OrderEntityTestKit service = OrderEntityTestKit.of(OrderEntity::new);
    OrderApi.Order order =
        OrderApi.Order.newBuilder()
            .setOrderId("O1234")
            .setProductId("P123")
            .setCustomerId("C001")
            .setQuantity(42)
            .build();
    ValueEntityResult<Empty> createResult = service.create(order);
    assertEquals(Empty.getDefaultInstance(), createResult.getReply());
    OrderDomain.OrderState currentState = service.getState();
    assertEquals("O1234", currentState.getOrderId());
    assertEquals("P123", currentState.getProductId());
    assertEquals("C001", currentState.getCustomerId());
    assertEquals(42, currentState.getQuantity());
    assertNotNull(currentState.getCreated());
    ValueEntityResult<OrderApi.Order> getResult =
        service.get(OrderApi.GetOrder.newBuilder().setOrderId("O1234").build());
    assertEquals(order, getResult.getReply());
  }
}
