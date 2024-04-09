/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.views;

import com.example.wiring.valueentities.customer.CustomerEntity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.annotations.ViewId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.util.List;


@ViewId("view_customers_by_creation_time")
@Table("customers_by_creation_time")
@Subscribe.ValueEntity(CustomerEntity.class)
public class CustomerByCreationTime extends View<CustomerEntity.Customer> {

  public record CustomerList(List<CustomerEntity.Customer> customers){}
  public static class ByTimeRequest {
    final public Instant createdOn;

    @JsonCreator
    public ByTimeRequest(@JsonProperty("createdOn") Instant createdOn) {
      this.createdOn = createdOn;
    }
  }

  @PostMapping("/customers/by_creation_time")
  @Query("SELECT * as customers FROM customers_by_creation_time WHERE createdOn >= :createdOn")
  public CustomerList getCustomerByTime(@RequestBody ByTimeRequest request) {
    return null;
  }

}

