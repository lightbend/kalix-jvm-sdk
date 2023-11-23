package user.registry.entity;


import kalix.javasdk.StatusCode;
import kalix.javasdk.annotations.*;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import user.registry.common.Done;
import user.registry.domain.*;

/**
 * Entity wrapping a User.
 * <p>
 * The UserEntity is part of the application layer. It implements the glue between the domain layer (user) and Kalix.
 * Incoming commands are delivered to the UserEntity, which passes them to the domain layer.
 * The domain layer returns the events that need to be persisted. The entity wraps them in an {@link Effect} that describes
 * to Kalix what needs to be done, e.g.: emit events, reply to the caller, etc.
 * <p>
 * A User has a name, a country and an email address.
 * The email address must be unique across all existing users. This is achieved with a choreography saga which ensures that
 * a user is only created if the email address is not already reserved.
 * <p>
 * This entity is protected from outside access. It can only be accessed from within this service (see the ACL annotation).
 * External access is gated and should go through the ApplicationController.
 */
@Id("id")
@TypeId("user")
@RequestMapping("/users/{id}")
@Acl(allow = @Acl.Matcher(service = "*"))
public class UserEntity extends EventSourcedEntity<User, User.UserEvent> {

  private final Logger logger = LoggerFactory.getLogger(getClass());


  @PostMapping
  public Effect<Done> createUser(@RequestBody User.Create cmd) {

    // since the user creation depends on the email address reservation, a better place to valid an incoming command
    // would be in the ApplicationController where we coordinate the two operations.
    // However, to demonstrate a failure case, we validate the command here.
    // As such, we can simulate the situation where an email address is reserved, but we fail to create the user.
    // When that happens the timer defined by the UniqueEmailSubscriber will fire and cancel the email address reservation.
    if (cmd.name() == null) {
      return effects().error("Name is empty", StatusCode.ErrorCode.BAD_REQUEST);
    }

    if (currentState() != null) {
      return effects().reply(Done.done());
    }

    logger.info("Creating user {}", cmd);
    return effects()
      .emitEvent(User.onCommand(cmd))
      .thenReply(__ -> Done.done());
  }

  @PutMapping("/change-email")
  public Effect<Done> changeEmail(@RequestBody User.ChangeEmail cmd) {
    if (currentState() == null) {
      return effects().error("User not found", StatusCode.ErrorCode.NOT_FOUND);
    }
    return effects()
      .emitEvents(currentState().onCommand(cmd))
      .thenReply(__ -> Done.done());
  }


  @GetMapping
  public Effect<User> getState() {
    if (currentState() == null) {
      return effects().error("User not found", StatusCode.ErrorCode.NOT_FOUND);
    }
    return effects().reply(currentState());
  }

  @EventHandler
  public User onEvent(User.UserWasCreated evt) {
    return User.onEvent(evt);
  }

  @EventHandler
  public User onEvent(User.EmailAssigned evt) {
    return currentState().onEvent(evt);
  }


  @EventHandler
  public User onEvent(User.EmailUnassigned evt) {
    return currentState();
  }

}
