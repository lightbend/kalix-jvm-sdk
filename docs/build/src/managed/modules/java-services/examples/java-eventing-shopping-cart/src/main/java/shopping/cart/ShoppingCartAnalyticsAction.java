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

package shopping.cart;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.Handler;
import com.google.protobuf.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.domain.ShoppingCartDomain;

/**
 * This action illustrates the consumption from a topic (shopping-cart-events) Incoming messages are
 * sent to log with no further processing.
 */
@Action
public class ShoppingCartAnalyticsAction {

  private static final Logger LOG = LoggerFactory.getLogger(ShoppingCartAnalyticsAction.class);

  @Handler
  public Empty processAdded(ShoppingCartDomain.ItemAdded event) {
    LOG.info("Analytics: item added '{}'", event);
    return Empty.getDefaultInstance();
  }

  @Handler
  public Empty processRemoved(ShoppingCartDomain.ItemRemoved event) {
    LOG.info("Analytics: item removed '{}'", event);
    return Empty.getDefaultInstance();
  }

  @Handler
  public Empty processCheckedOut(ShoppingCartDomain.CheckedOut event) {
    LOG.info("Analytics: cart checked out '{}'", event);
    return Empty.getDefaultInstance();
  }
}
