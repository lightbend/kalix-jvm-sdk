package store.view.joined;

import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.annotations.ViewId;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;
import store.customer.api.CustomerEntity;
import store.customer.domain.CustomerEvent;
import store.order.api.OrderEntity;
import store.order.domain.Order;
import store.product.api.ProductEntity;
import store.product.domain.ProductEvent;
import store.view.model.Customer;
import store.view.model.Product;

// tag::join[]
@ViewId("joined-customer-orders") // <1>
public class JoinedCustomerOrdersView {

  @GetMapping("/joined-customer-orders/{customerId}")
  @Query( // <2>
      """
      SELECT *
      FROM customers
      JOIN orders ON customers.customerId = orders.customerId
      JOIN products ON products.productId = orders.productId
      WHERE customers.customerId = :customerId
      ORDER BY orders.createdTimestamp
      """)
  public Flux<CustomerOrder> get(String customerId) { // <3>
    return null;
  }

  @Table("customers") // <4>
  @Subscribe.EventSourcedEntity(CustomerEntity.class)
  public static class Customers extends View<Customer> {
    public UpdateEffect<Customer> onEvent(CustomerEvent.CustomerCreated created) {
      String id = updateContext().eventSubject().orElse("");
      return effects()
          .updateState(new Customer(id, created.email(), created.name(), created.address()));
    }

    public UpdateEffect<Customer> onEvent(CustomerEvent.CustomerNameChanged event) {
      return effects().updateState(viewState().withName(event.newName()));
    }

    public UpdateEffect<Customer> onEvent(CustomerEvent.CustomerAddressChanged event) {
      return effects().updateState(viewState().withAddress(event.newAddress()));
    }
  }

  @Table("products") // <4>
  @Subscribe.EventSourcedEntity(ProductEntity.class)
  public static class Products extends View<Product> {
    public UpdateEffect<Product> onEvent(ProductEvent.ProductCreated created) {
      String id = updateContext().eventSubject().orElse("");
      return effects().updateState(new Product(id, created.name(), created.price()));
    }

    public UpdateEffect<Product> onEvent(ProductEvent.ProductNameChanged event) {
      return effects().updateState(viewState().withProductName(event.newName()));
    }

    public UpdateEffect<Product> onEvent(ProductEvent.ProductPriceChanged event) {
      return effects().updateState(viewState().withPrice(event.newPrice()));
    }
  }

  @Table("orders") // <4>
  @Subscribe.ValueEntity(OrderEntity.class)
  public static class Orders extends View<Order> {}
}
// end::join[]
