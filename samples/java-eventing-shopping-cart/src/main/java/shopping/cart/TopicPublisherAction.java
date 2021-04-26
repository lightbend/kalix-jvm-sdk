/*
 * Copyright 2019 Lightbend Inc.
 */

package shopping.cart;

import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shopping.cart.domain.ShoppingCartDomain;

@Action
public class TopicPublisherAction {
  private static final Logger LOG = LoggerFactory.getLogger(TopicPublisherAction.class);

  @Handler
  public ShoppingCartDomain.ItemAdded publishAdded(ShoppingCartDomain.ItemAdded in) {
    LOG.info("Publishing: '{}' to topic", in);
    return in;
  }

  @Handler
  public ShoppingCartDomain.ItemRemoved publishRemoved(ShoppingCartDomain.ItemRemoved in) {
    LOG.info("Publishing: '{}' to topic", in);
    return in;
  }
}
