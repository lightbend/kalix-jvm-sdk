/*
 * Copyright 2021 Lightbend Inc.
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

package kalix.springsdk.valueentity;

import kalix.springsdk.annotations.EntityKey;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

public class RestAnnotatedValueEntities {

  @EntityKey({"userId", "cartId"})
  @RequestMapping("/user/{userId}/{cartId}")
  public static class PostWithEntityKeys extends ValueEntity<User> {
    @Override
    public User emptyState() {
      return User.empty();
    }

    @PostMapping("/create")
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }
  }

  @EntityKey({"userId", "cartId"})
  @RequestMapping("/user/{userId}/{cartId}")
  public static class ValueEntityUsingJavaSdk extends kalix.javasdk.valueentity.ValueEntity<User> {
    @Override
    public User emptyState() {
      return User.empty();
    }

    @PostMapping("/create")
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }
  }
}
