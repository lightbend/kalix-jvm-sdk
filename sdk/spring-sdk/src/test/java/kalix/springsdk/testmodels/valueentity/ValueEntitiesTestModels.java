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

package kalix.springsdk.testmodels.valueentity;

import kalix.javasdk.valueentity.ValueEntity;
import kalix.springsdk.annotations.Acl;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.testmodels.Done;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

public class ValueEntitiesTestModels {

  @EntityKey( {"userId", "cartId"})
  @EntityType("user")
  @RequestMapping("/user/{userId}/{cartId}")
  public static class PostWithEntityKeys extends ValueEntity<User> {
    @Override
    public User emptyState() {
      return null;
    }

    @PostMapping("/create")
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }
  }

  @EntityKey( {"userId", "cartId"})
  @EntityType("user")
  @Acl(allow = @Acl.Matcher(service = "test"))
  public static class ValueEntityWithServiceLevelAcl extends ValueEntity<User> {
  }

  @EntityKey( {"userId", "cartId"})
  @EntityType("user")
  public static class ValueEntityWithMethodLevelAcl extends ValueEntity<User> {
    @PostMapping("/create")
    @Acl(allow = @Acl.Matcher(service = "test"))
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }
  }
}
