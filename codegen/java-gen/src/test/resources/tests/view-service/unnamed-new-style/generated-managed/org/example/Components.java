package org.example;

import kalix.javasdk.DeferredCall;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

/**
 * Not intended for user extension, provided through generated implementation
 */
public interface Components {
  UserByNameViewCalls userByNameView();

  interface UserByNameViewCalls {
    DeferredCall<org.example.unnamed.view.UserViewModel.ByNameRequest, org.example.unnamed.view.UserViewModel.UserResponse> getUserByName(org.example.unnamed.view.UserViewModel.ByNameRequest byNameRequest);
  }
}
