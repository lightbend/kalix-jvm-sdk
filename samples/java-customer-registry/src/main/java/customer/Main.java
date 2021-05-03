/*
 * Copyright 2021 Lightbend Inc.
 */

package customer;

import com.akkaserverless.javasdk.AkkaServerless;
import customer.api.CustomerApi;
import customer.domain.CustomerDomain;
import customer.view.CustomerViewModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    LOG.info("started");
    if (args.length == 0) {
      // This is for value entity
      // tag::register[]
      new AkkaServerless()
          .registerView(
              CustomerViewModel.getDescriptor().findServiceByName("CustomerByName"),
              "customerByName",
              CustomerDomain.getDescriptor())
          // end::register[]
          .registerValueEntity(
              CustomerValueEntity.class,
              CustomerApi.getDescriptor().findServiceByName("CustomerService"),
              CustomerDomain.getDescriptor())
          .start()
          .toCompletableFuture()
          .get();
    } else {
      // This is for event sourced entity
      // tag::register-with-class[]
      new AkkaServerless()
          .registerView(
              CustomerView.class,
              CustomerViewModel.getDescriptor().findServiceByName("CustomerByNameView"),
              "customerByName",
              CustomerDomain.getDescriptor())
          // end::register-with-class[]
          .registerEventSourcedEntity(
              CustomerEventSourcedEntity.class,
              CustomerApi.getDescriptor().findServiceByName("CustomerService"),
              CustomerDomain.getDescriptor())
          .start()
          .toCompletableFuture()
          .get();
    }
  }
}
