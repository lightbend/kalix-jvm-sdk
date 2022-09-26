package customer.view;
/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

import customer.api.CustomerEntity;
import customer.api.CustomerEvent;
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import org.springframework.web.bind.annotation.GetMapping;

@Table("customers_by_name")
public class CustomerByNameView extends View<CustomerView> {

  @GetMapping("/customer/by_name/{customer_name}")
  @Query("SELECT * FROM customers_by_name WHERE name = :customer_name")
  public CustomerView getCustomer(String name) {
    return null;
  }

  @Subscribe.EventSourcedEntity(CustomerEntity.class)
  public UpdateEffect<CustomerView> onEvent(CustomerView customer, CustomerEvent event) {
    return CustomerView.onEvent(customer, event)
        .map(state ->effects().updateState(state))
        .orElse(effects().ignore());
  }
}
