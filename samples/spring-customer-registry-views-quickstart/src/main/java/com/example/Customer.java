package com.example;
/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

public class Customer {
  public String customerId;
  public String email;
  public String name;
  public Address address;

  public Customer(String customerId, String email, String name, Address address) {
    this.customerId = customerId;
    this.email = email;
    this.name = name;
    this.address = address;
  }


}
