/* This code was generated by Akka Serverless tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example.actions;

import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.google.protobuf.Empty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// tag::counter-topic-sub[]
public class CounterTopicSubscriptionAction extends AbstractCounterTopicSubscriptionAction {

  private Logger logger = LoggerFactory.getLogger(getClass());

  public CounterTopicSubscriptionAction(ActionCreationContext creationContext) {}

  @Override
  public Effect<Empty> increase(CounterTopicApi.Increased increased) { //<1> 
    logger.info("Received increase event: " + increased.toString());
    return effects().noReply();
  }

  @Override
  public Effect<Empty> decrease(CounterTopicApi.Decreased decreased) {  
    logger.info("Received increase event: " + decreased.toString());
    return effects().noReply();
  }
}
// end::counter-topic-sub[]