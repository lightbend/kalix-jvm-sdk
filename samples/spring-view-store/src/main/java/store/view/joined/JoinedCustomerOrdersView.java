package store.view.joined;

import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.annotations.ViewId;
// tag::join[]
import kalix.springsdk.view.MultiTableView;
import kalix.springsdk.view.ViewTable;
// end::join[]
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
public class JoinedCustomerOrdersView extends MultiTableView { // <2>
  // end::updates[]

  // tag::query[]
  @GetMapping("/joined-customer-orders/{customerId}")
  @Query( // <3>
      """
      SELECT *
      FROM customers
      JOIN orders ON customers.customerId = orders.customerId
      JOIN products ON products.productId = orders.productId
      WHERE customers.customerId = :customerId
      ORDER BY orders.createdTimestamp
      """)
  public Flux<CustomerOrder> get(String customerId) { // <4>
    return null;
  }

  @Table("customers") // <5>
  @Subscribe.EventSourcedEntity(CustomerEntity.class)
  public static class Customers extends ViewTable<Customer> {
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

  @Table("products") // <5>
  @Subscribe.EventSourcedEntity(ProductEntity.class)
  public static class Products extends ViewTable<Product> {
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

  @Table("orders") // <5>
  @Subscribe.ValueEntity(OrderEntity.class)
  public static class Orders extends ViewTable<Order> {}
}
// end::join[]
