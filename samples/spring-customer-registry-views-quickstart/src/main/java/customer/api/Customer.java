package customer.api;
/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

public record Customer(String customerId, String email, String name, Address address) {

  public Customer withName(String newName){
    return new Customer(customerId, email, newName, address);
  }

  public Customer withAddress(Address newAddress){
    return new Customer(customerId, email, name, newAddress);
  }
}
