package user.registry.subscriber;


import com.typesafe.config.Config;
import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.client.ComponentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import user.registry.common.Done;
import user.registry.domain.UniqueEmail;
import user.registry.entity.UniqueEmailEntity;

import java.time.Duration;

/**
 * This Action plays the role of a subscriber to the UniqueEmailEntity state changes.
 * <p>
 * In the choreography, this subscriber will react to state changes from the UniqueEmailEntity.
 * <p>
 * When it sees an email address that is not confirmed, it will schedule a timer to fire in 10 seconds.
 * When it sees an email address that is confirmed, it will delete the timer (if it exists).
 * <p>
 * Note:
 * This is just an example of how to use timers. In a real application, you would probably want to use much longer timeout.
 * We use 10 seconds here to make the example easier to test locally.
 * <p>
 * Also, strictly speaking, we don't need to delete the timer when the email address is confirmed. If we don't delete it and the timer fires,
 * the UniqueEmailEntity will just ignore the message. But it is a good practice to clean up obsolete times and save resources.
 */
@Subscribe.ValueEntity(UniqueEmailEntity.class)
public class UniqueEmailSubscriber extends Action {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private final ComponentClient client;
  private final Config config;

  public UniqueEmailSubscriber(ComponentClient client, Config config) {
    this.client = client;
    this.config = config;
  }

  public Effect<Done> onChange(UniqueEmail email) {

    logger.info("Received update for address '{}'", email);
    var timerId = "timer-" + email.address();

    if (email.isReserved()) {
      // by default the timer will fire after 2h (see settings in src/resources/application.conf)
      // but we can override these settings using a -D argument
      // for example, calling: mvn kalix:runAll -Demail.confirmation.timeout=10s will make the timer fire after 10 seconds
      Duration delay = config.getDuration("email.confirmation.timeout");
      logger.info("Email is not confirmed, scheduling timer '{}' to fire in '{}'", timerId, delay);
      var callToUnReserve =
        client
          .forValueEntity(email.address())
          .call(UniqueEmailEntity::cancelReservation);

      var timer = timers().startSingleTimer(
        timerId,
        delay,
        callToUnReserve);

      return effects().asyncReply(timer.thenApply(__ -> Done.done()));

    } else if (email.isConfirmed()) {
      logger.info("Email is already confirmed, deleting timer (if exists) '{}'", timerId);
      var cancellation = timers().cancel(timerId);
      return effects().asyncReply(cancellation.thenApply(__ -> Done.done()));

    } else {
      // Email is not reserved, so we don't need to do anything
      return effects().reply(Done.done());
    }
  }

}
