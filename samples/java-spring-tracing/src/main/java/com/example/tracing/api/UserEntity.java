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
import org.springframework.web.bind.annotation.*;

@Id("userId")
@TypeId("user")
@RequestMapping("/user/{userId}")
@ForwardHeaders("traceparent")
public class UserEntity extends EventSourcedEntity<User, UserEvent> {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(UserEntity.class);

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
  public EventSourcedEntity.Effect<String> updateName(@RequestBody UserCmd.UpdateNameCmd updateNameCmd) {
    if (currentState() == emptyState()){
      return effects().error("User does not exist");
    }

    var updated = new UserEvent.UserNameUpdated(updateNameCmd.name);

    return effects()
        .emitEvent(updated)
        .thenReply(newState -> "OK");
  }

  @PutMapping("/photo")
  public EventSourcedEntity.Effect<String> updatePhoto(@RequestParam String url) {
    if (currentState() == emptyState()){
      return effects().error("User does not exist");
    }

    var updated = new UserEvent.UserPhotoUpdated(url);
    return effects()
        .emitEvent(updated)
        .thenReply(newState -> "OK");
  }

  @EventHandler
  public User handleEvent(UserEvent event) {
    if (event instanceof UserEvent.UserAdded userAdded) {
      log.info("User added: {}", userAdded.email());
      return new User(entityId, "", userAdded.email(), "");
    } else if (event instanceof UserEvent.UserNameUpdated nameUpdated) {
      log.info("User name updated: {}", nameUpdated.name());
      return currentState().withName(nameUpdated.name());
    } else if (event instanceof UserEvent.UserPhotoUpdated nameUpdated) {
      log.info("User photo updated: {}", nameUpdated.url());
      return currentState().withPhoto(nameUpdated.url());
    } else {
      return currentState();
    }
  }

}
