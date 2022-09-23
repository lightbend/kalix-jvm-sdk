package customer.api;
/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Address {
  public String street;
  public String city;

  @JsonCreator
  public Address(@JsonProperty("street") String street, @JsonProperty("city") String city) {
    this.street = street;
    this.city = city;
  }
}
