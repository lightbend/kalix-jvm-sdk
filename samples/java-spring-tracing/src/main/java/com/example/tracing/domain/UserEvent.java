package com.example.tracing.domain;


import kalix.javasdk.annotations.TypeName;
public sealed interface UserEvent {

  @TypeName("user-added")
  record UserAdded(String email) implements UserEvent {}

  @TypeName("name-updated")
  record UserNameUpdated(String name) implements UserEvent {}

  @TypeName("photo-updated")
  record UserPhotoUpdated(String url) implements UserEvent {}
}
