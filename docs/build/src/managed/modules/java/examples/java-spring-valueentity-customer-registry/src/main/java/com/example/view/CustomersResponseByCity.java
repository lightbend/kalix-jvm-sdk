package com.example.view;

import com.example.api.CustomerEntity;
import com.example.api.CustomersResponse;
import com.example.domain.Customer;
import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.annotations.ViewId;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@ViewId("view_response_customers_by_city")
@Table("customers_by_city")
@Subscribe.ValueEntity(CustomerEntity.class)
public class CustomersResponseByCity extends View<Customer> {

  @GetMapping("/wrapped/by_city")
  @Query("""
    SELECT * AS customers
      FROM customers_by_city
      WHERE address.city = ANY(:cities)
    """)
  public CustomersResponse getCustomers(@RequestParam List<String> cities) {
    return null;
  }
}
