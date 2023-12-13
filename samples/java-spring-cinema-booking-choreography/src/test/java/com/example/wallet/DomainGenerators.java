package com.example.wallet;

import java.util.UUID;

public class DomainGenerators {
  public static String randomId() {
    return UUID.randomUUID().toString();
  }
}
