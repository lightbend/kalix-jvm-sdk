package com.example;
/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import org.springframework.web.bind.annotation.GetMapping;

@Table("customers_by_email")
@Subscribe.ValueEntity(CustomerEntity.class)
public class CustomerByEmailView extends View<Customer> {

  // FIXME should not actually be needed
  @Override
  public Customer emptyState() {
    return null;
  }

  @GetMapping("/customer/by_email/{email}")
  @Query("SELECT * FROM customers_by_email WHERE email = :email")
  public Customer getCustomer(String email) {
    return null;
  }
}
