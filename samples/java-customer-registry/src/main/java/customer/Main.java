/*
 * Copyright 2019 Lightbend Inc.
 */

package customer;

import com.akkaserverless.javasdk.AkkaServerless;
import customer.api.CustomerApi;
import customer.domain.CustomerDomain;
import customer.view.CustomerViewModel;

public final class Main {
  public static void main(String[] args) throws Exception {
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
