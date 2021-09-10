package com.example.counter;

import com.akkaserverless.javasdk.AkkaServerless;
import com.example.counter.domain.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static AkkaServerless createAkkaServerless() {
    return AkkaServerlessFactory.withComponents(Counter::new);
  }

  public static void main(String[] args) throws Exception {
    LOG.info("starting the Akka Serverless service");
    createAkkaServerless().start();
  }
}
