/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package com.example.wiring.valueentities.user;

import kalix.javasdk.Metadata;
import kalix.javasdk.StatusCode;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.valueentity.ValueEntityContext;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@Id("id")
@TypeId("user")
@RequestMapping("/user/{id}")
public class UserEntity extends ValueEntity<User> {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private final ValueEntityContext context;

  public UserEntity(ValueEntityContext context) {
    this.context = context;
  }

  @GetMapping
  public Effect<User> getUser() {
    if (currentState() == null)
      return effects().error("User not found", StatusCode.ErrorCode.NOT_FOUND);

    return effects().reply(currentState());
  }

  @PostMapping("/{email}/{name}")
  public Effect<String> createOrUpdateUser(@PathVariable String email, @PathVariable String name) {
    return effects().updateState(new User(email, name)).thenReply("Ok",
        Metadata.EMPTY.withStatusCode(StatusCode.Success.CREATED));
  }

  @PutMapping("/{email}/{name}")
  public Effect<String> createUser(@PathVariable String email, @PathVariable String name) {
    return effects().updateState(new User(email, name)).thenReply("Ok from put");
  }

  @PatchMapping("/email/{email}")
  public Effect<String> updateEmail(@PathVariable String email) {
    return effects().updateState(new User(email, currentState().name)).thenReply("Ok from patch");
  }

  @PatchMapping("/email")
  public Effect<String> updateEmailFromReqParam(@RequestParam String email) {
    return effects().updateState(new User(email, currentState().name)).thenReply("Ok from patch");
  }

  @DeleteMapping
  public Effect<String> deleteUser() {
    return effects().deleteEntity().thenReply("Ok from delete");
  }

  @PostMapping("/restart")
  public EventSourcedEntity.Effect<Integer> restart() { // force entity restart, useful for testing
    logger.info(
        "Restarting counter with commandId={} commandName={} current={}",
        commandContext().commandId(),
        commandContext().commandName(),
        currentState());

    throw new RuntimeException("Forceful restarting entity!");
  }
}
