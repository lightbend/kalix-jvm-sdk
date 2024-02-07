package com.example.tracing.domain;

public record User(String userId, String name, String email) {


  public User withName(String name) {
    return new User(userId, name, email);
  }
}

