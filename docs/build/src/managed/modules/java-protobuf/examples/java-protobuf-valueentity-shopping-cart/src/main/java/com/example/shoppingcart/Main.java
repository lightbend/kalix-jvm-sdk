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
package com.example.shoppingcart;

import com.example.shoppingcart.domain.ShoppingCartProvider;
import kalix.javasdk.Kalix;
import com.example.shoppingcart.domain.ShoppingCart;
import kalix.javasdk.action.ActionOptions;
import kalix.javasdk.valueentity.ValueEntityOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

// tag::forward-headers[]
public final class Main {
  // end::forward-headers[]
  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  // tag::forward-headers[]
  public static Kalix createKalix() {
    Kalix kalix = new Kalix();
    ActionOptions forwardHeaders = ActionOptions.defaults()
        .withForwardHeaders(Set.of("UserRole")); // <1>
    return kalix
        .register(ShoppingCartActionProvider.of(ShoppingCartActionImpl::new).
            withOptions(forwardHeaders)) // <2>
        // end::forward-headers[]
        .register(ShoppingCartProvider.of(ShoppingCart::new).withOptions(ValueEntityOptions.defaults().withForwardHeaders(Set.of("Role"))));
    // tag::forward-headers[]
  }

  public static void main(String[] args) throws Exception {
    LOG.info("starting the Kalix service");
    createKalix().start();
  }
}
// end::forward-headers[]
