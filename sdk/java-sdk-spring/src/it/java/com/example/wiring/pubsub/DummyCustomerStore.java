/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.pubsub;

import com.example.wiring.valueentities.customer.CustomerEntity.Customer;

import java.util.concurrent.ConcurrentHashMap;

public class DummyCustomerStore {

  private static ConcurrentHashMap<String, Customer> customers = new ConcurrentHashMap<>();

  public static void store(String storeName, String entityId, Customer customer) {
    customers.put(storeName + "-" + entityId, customer);
  }

  public static Customer get(String storeName, String entityId) {
    return customers.get(storeName + "-" + entityId);
  }
}
