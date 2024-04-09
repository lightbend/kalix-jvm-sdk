/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.valueentity;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.TypeId;
import kalix.spring.testmodels.Done;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Id("id")
@TypeId("user")
@RequestMapping("/user/{id}")
public class UserEntity extends ValueEntity<User> {
  @Override
  public User emptyState() {
    return null;
  }

  @PostMapping("/create")
  public ValueEntity.Effect<Done> createUser(@RequestBody CreateUser createUser) {
    return effects().reply(Done.instance);
  }
}
