package org.example.view;

import kalix.javasdk.view.View;
import kalix.javasdk.view.ViewContext;

// This class was initially generated based on the .proto definition by Kalix tooling.
// This is the implementation for the View Service described in your example-views.proto file.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public class UserByNameViewImpl extends AbstractUserByNameView {

  public UserByNameViewImpl(ViewContext context) {}

  @Override
  public UserViewModel.UserState emptyState() {
    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state");
  }

  @Override
  public View.UpdateEffect<UserViewModel.UserState> updateCustomer(
      UserViewModel.UserState state,
      UserViewModel.UserState userState) {
    throw new UnsupportedOperationException("Update handler for 'UpdateCustomer' not implemented yet");
  }

}

