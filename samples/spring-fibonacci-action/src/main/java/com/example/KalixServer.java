package com.example;

import com.example.fibonacci.FibonacciAction;
import kalix.javasdk.Kalix;
import kalix.springsdk.action.ReflectiveActionProvider;
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
    kalix.register(ReflectiveActionProvider.of(FibonacciAction.class, __ -> new FibonacciAction()));
    kalix.start();
  }
}
