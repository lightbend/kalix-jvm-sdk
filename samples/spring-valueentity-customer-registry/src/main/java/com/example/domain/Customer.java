package com.example.domain;

import java.time.Instant;
import java.util.List;

public record Customer(String customerId, String email, String name, Address address, double createdAt, Integer age) { // <1>

  public Customer(String customerId, String email, String name, Address address) {
    this(customerId, email, name, address, Instant.now().toEpochMilli(), 18);
  }

  public Customer withName(String newName) { // <2>
    return new Customer(customerId, email, newName, address);
  }

  public Customer withAddress(Address newAddress) { // <2>
    return new Customer(customerId, email, name, newAddress);
  }
}
