/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.wiring.views;

import com.example.wiring.valueentities.customer.CustomerEntity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;
import kalix.springsdk.annotations.ViewId;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.time.Instant;


@ViewId("view_customers_by_creation_time")
@Table("customers_by_creation_time")
@Subscribe.ValueEntity(CustomerEntity.class)
public class CustomerByCreationTime extends View<CustomerEntity.Customer> {

  public static class ByTimeRequest {
    final public Instant createdOn;

    @JsonCreator
    public ByTimeRequest(@JsonProperty("createdOn") Instant createdOn) {
      this.createdOn = createdOn;
    }
  }

  @PostMapping("/customers/by_creation_time")
  @Query("SELECT * FROM customers_by_creation_time WHERE createdOn >= :createdOn")
  public Flux<CustomerEntity.Customer> getCustomerByTime(@RequestBody ByTimeRequest request) {
    return null;
  }

}

