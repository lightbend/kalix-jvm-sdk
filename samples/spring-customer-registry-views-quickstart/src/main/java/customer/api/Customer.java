package customer.api;
/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Customer {
  public String customerId;
  public String email;
  public String name;
  public Address address;

  // TODO: remove JsonCreator and JsonProperty
  // this should not be needed and it's not when running the application
  // however, the integration tests seems to need it. 
  // Probably related to how the compiler is configured for the tests?
  @JsonCreator
  public Customer(@JsonProperty("customerId") String customerId,
                  @JsonProperty("email") String email,
                  @JsonProperty("name") String name,
                  @JsonProperty("address") Address address) {
    this.customerId = customerId;
    this.email = email;
    this.name = name;
    this.address = address;
  }


}
