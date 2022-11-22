package org.example.view;

import kalix.javasdk.impl.view.UpdateHandlerNotFound;
import kalix.javasdk.impl.view.ViewRouter;
import kalix.javasdk.view.View;

// This code is managed by Kalix tooling.
// It will be re-generated to reflect any changes to your protobuf definitions.
// DO NOT EDIT

public class UserByNameViewRouter extends ViewRouter<UserByNameViewImpl> {

  public UserByNameViewRouter(UserByNameViewImpl view) {
    super(view);
  }

  @Override
  public <S> View.UpdateEffect<S> handleUpdate(
      String eventName,
      S state,
      Object event) {

    switch (eventName) {

      default:
        throw new UpdateHandlerNotFound(eventName);
    }
  }

}

