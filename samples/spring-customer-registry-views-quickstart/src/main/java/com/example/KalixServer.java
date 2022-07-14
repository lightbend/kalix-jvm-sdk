package com.example;

import kalix.javasdk.Kalix;
import kalix.springsdk.valueentity.ReflectiveValueEntityProvider;
import kalix.springsdk.view.ReflectiveViewProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

// TODO: this should be part of the Spring SDK and automagically find the components to register
@Component
public class KalixServer {

  private static final Logger logger = LoggerFactory.getLogger(KalixServer.class);

  @PostConstruct
  public void init() {
    logger.info("Starting Kalix!");
    Kalix kalix = new Kalix();
    kalix.register(ReflectiveValueEntityProvider.of(CustomerEntity.class, __ -> new CustomerEntity()))
      .register(ReflectiveViewProvider.of(CustomerByEmailView.class, __ -> new CustomerByEmailView()))
      .register(ReflectiveViewProvider.of(CustomerByNameView.class, __ -> new CustomerByNameView()));
    kalix.start();
  }
}
