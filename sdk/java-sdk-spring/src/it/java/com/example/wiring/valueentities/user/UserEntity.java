/*
 * Copyright 2024 Lightbend Inc.
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
