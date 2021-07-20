/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package customer;

import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.Reply;
import com.akkaserverless.javasdk.impl.AnySupport;
import com.akkaserverless.javasdk.lowlevel.ValueEntityFactory;
import com.akkaserverless.javasdk.lowlevel.ValueEntityHandler;
import com.akkaserverless.javasdk.lowlevel.ViewFactory;
import com.akkaserverless.javasdk.lowlevel.ViewUpdateHandler;
import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.akkaserverless.javasdk.view.UpdateHandlerContext;
import com.akkaserverless.javasdk.view.ViewContext;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
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
          .lowLevel()
          .registerValueEntity(
              context -> new CustomerValueEntityHandler(),
              CustomerValueEntityHandler.serviceDescriptor,
              CustomerValueEntityHandler.entityType,
              ValueEntityOptions.defaults())
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
