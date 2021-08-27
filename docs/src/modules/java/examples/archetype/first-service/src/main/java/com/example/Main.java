package com.example;

import com.akkaserverless.javasdk.AkkaServerless;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.domain.Counter;

public final class Main {
    
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static final AkkaServerless createAkkaServerless() {
        // The AkkaServerlessFactory automatically registers any generated Actions, Views or Entities,
        // and is kept up-to-date with any changes in your protobuf definitions.
        // If you prefer, you may remove this and manually register these components in a
        // `new AkkaServerless()` instance.
        return AkkaServerlessFactory.withComponents(Counter::new);
    
    public static void main(String[] args) throws Exception {
        LOG.info("starting the Akka Serverless service");
        createAkkaServerless().start();
    }
}
