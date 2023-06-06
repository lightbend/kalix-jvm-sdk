/* This code was generated by Kalix tooling.
 * As long as this file exists it will not be re-generated.
 * You are free to make changes to this file.
 */

package com.example;

import com.example.actions.CounterJournalToTopicWithMetaAction;
import kalix.javasdk.Kalix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.actions.CounterJournalToTopicAction;
import com.example.actions.CounterTopicSubscriptionAction;
import com.example.actions.CounterCommandFromTopicAction;
import com.example.domain.Counter;

public final class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  public static Kalix createKalix() {
    // The KalixFactory automatically registers any generated Actions, Views or Entities,
    // and is kept up-to-date with any changes in your protobuf definitions.
    // If you prefer, you may remove this and manually register these components in a
    // `new Kalix()` instance.
    return KalixFactory.withComponents(
      Counter::new,
      CounterCommandFromTopicAction::new,
      CounterJournalToTopicAction::new,
      CounterJournalToTopicWithMetaAction::new,
      CounterTopicSubscriptionAction::new);
  }

  public static void main(String[] args) throws Exception {
    LOG.info("starting the Kalix service");
    createKalix().start();
  }
}