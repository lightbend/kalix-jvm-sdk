package com.example;

import com.example.actions.CounterStateSubscriptionAction;
import com.example.actions.DoubleCounterAction;
import com.example.actions.ExternalCounterAction;
import com.example.domain.Counter;
import com.example.user.UserEntity;
import com.example.user.view.UserByEmailView;
import com.example.user.view.UserByNameView;
import kalix.javasdk.Kalix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static Kalix createKalix() {
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `new Kalix()` instance.
    return KalixFactory.withComponents(
      Counter::new,
      UserEntity::new,
      CounterStateSubscriptionAction::new,
      DoubleCounterAction::new,
      ExternalCounterAction::new,
      UserByEmailView::new,
      UserByNameView::new);
  }

  public static void main(String[] args) throws Exception {
    LOG.info("starting the Kalix service");
    createKalix().start();
  }
}
