package com.example.tracing.api;

import com.example.tracing.domain.User;
import com.example.tracing.domain.UserEvent;
import kalix.javasdk.StatusCode;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.ForwardHeaders;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.eventsourcedentity.EventSourcedEntityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@Id("userId")
@TypeId("user")
@RequestMapping("/user/{userId}")
@ForwardHeaders("traceparent")
public class UserEntity extends EventSourcedEntity<User, UserEvent> {

  private static final Logger log = LoggerFactory.getLogger(UserEntity.class);

  private final String entityId;

  public sealed interface UserCmd {
    record CreateCmd(String email) implements UserCmd { }
    record UpdateNameCmd(String name) implements UserCmd { }
    record UpdatePhotoCmd(String url) implements UserCmd { }
  }


  public UserEntity(EventSourcedEntityContext context) {
    this.entityId = context.entityId();
  }

  @Override
  public User emptyState() { // <2>
    return new User(entityId, "", "", "");
  }

  @GetMapping
  public Effect<User> get() {
    if (currentState().equals(emptyState()))
      return effects().error("User does not exist", StatusCode.ErrorCode.NOT_FOUND);

    return effects().reply(currentState());
  }

  @PostMapping("/add")
  public Effect<String> add(@RequestBody UserCmd.CreateCmd create) {
    log.info("Current context: {}, empty {}", currentState(), emptyState());
    if (!currentState().equals(emptyState())) {
      return effects().error("User already exists", StatusCode.ErrorCode.BAD_REQUEST);
    }

    var created = new UserEvent.UserAdded(create.email);

    return effects()
        .emitEvent(created)
        .thenReply(newState -> "OK");
  }

  @PutMapping("/name")
  public Effect<String> updateName(@RequestBody UserCmd.UpdateNameCmd updateNameCmd) {
    if (currentState().equals(emptyState())) {
      return effects().error("User does not exist");
    }

    var updated = new UserEvent.UserNameUpdated(updateNameCmd.name);

    return effects()
        .emitEvent(updated)
        .thenReply(newState -> "OK");
  }

  @PutMapping("/photo")
  public Effect<String> updatePhoto(@RequestParam String url) {
    if (currentState().equals(emptyState())) {
      return effects().error("User does not exist");
    }

    var updated = new UserEvent.UserPhotoUpdated(url);
    return effects()
        .emitEvent(updated)
        .thenReply(newState -> "OK");
  }

  @EventHandler
  public User handleEvent(UserEvent event) {
      return switch (event) {
          case UserEvent.UserAdded userAdded -> {
              log.info("User added: {}", userAdded.email());
              yield new User(entityId, "", userAdded.email(), "");
          }
          case UserEvent.UserNameUpdated nameUpdated -> {
              log.info("User name updated: {}", nameUpdated.name());
              yield currentState().withName(nameUpdated.name());
          }
          case UserEvent.UserPhotoUpdated photoUpdated -> {
              log.info("User photo updated: {}", photoUpdated.url());
              yield currentState().withPhoto(photoUpdated.url());
          }
      };
  }

}
