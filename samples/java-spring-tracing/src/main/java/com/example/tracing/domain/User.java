package com.example.tracing.domain;

public record User(String userId, String name, String email, String photoUrl) {

  public User withName(String name) {
    return new User(userId, name, email, photoUrl);
  }

  public User withPhoto(String photoUrl) {
    return new User(userId, name, email, photoUrl);
  }
}

