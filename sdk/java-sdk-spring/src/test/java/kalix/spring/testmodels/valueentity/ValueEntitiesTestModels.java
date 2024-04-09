/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.valueentity;

import kalix.javasdk.annotations.Acl;
import kalix.javasdk.annotations.Id;
import kalix.javasdk.annotations.JWT;
import kalix.javasdk.annotations.TypeId;
import kalix.javasdk.valueentity.ValueEntity;
import kalix.spring.testmodels.Done;
import org.springframework.web.bind.annotation.*;

public class ValueEntitiesTestModels {

  @Id( {"userId", "cartId"})
  @TypeId("user")
  @RequestMapping("/user/{userId}/{cartId}")
  public static class PostWithIds extends ValueEntity<User> {
    @Override
    public User emptyState() {
      return null;
    }

    @PostMapping("/create")
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }
  }

  @Id({"userId", "cartId"})
  @TypeId("user")
  @RequestMapping("/user/{cartId}/{userId}")
  public static class PostWithIdsIncorrectOrder extends ValueEntity<User> {
    @Override
    public User emptyState() {
      return null;
    }

    @PostMapping("/create")
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }
  }

  @Id({"userId", "cartId"})
  @TypeId("user")
  @RequestMapping("/user/{cartId}")
  public static class PostWithIdsMissingParams extends ValueEntity<User> {
    @Override
    public User emptyState() {
      return null;
    }

    @PostMapping("/create")
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }
  }

  @Id({"userId", "cartId"})
  @TypeId("user")
  @RequestMapping()
  public static class GetWithQueryParams extends ValueEntity<User> {
    @Override
    public User emptyState() {
      return null;
    }

    @PostMapping("/user/{userId}/{cartId}/create")
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }

    @PostMapping("/user/{userId}/{cartId}/create/{otherParam}")
    public ValueEntity.Effect<Done> createEntity2(@RequestParam String someParam,
                                                  @PathVariable Integer otherParam, @PathVariable String cartId, @RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }

    @GetMapping("/user/{userId}/{cartId}/get")
    public ValueEntity.Effect<Done> getUser(@RequestParam String someParam, @RequestParam Integer otherParam) {
      return effects().reply(Done.instance);
    }
  }

  @Id( {"userId", "cartId"})
  @TypeId("user")
  @Acl(allow = @Acl.Matcher(service = "test"))
  public static class ValueEntityWithServiceLevelAcl extends ValueEntity<User> {
  }

  @Id( {"userId", "cartId"})
  @TypeId("user")
  public static class ValueEntityWithMethodLevelAcl extends ValueEntity<User> {
    @PostMapping("/{userId}/{cartId}/create")
    @Acl(allow = @Acl.Matcher(service = "test"))
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }
  }

  @Id( {"userId", "cartId"})
  @TypeId("user")
  @JWT(
    validate = JWT.JwtMethodMode.BEARER_TOKEN,
    bearerTokenIssuer = {"a", "b"},
    staticClaims = {
      @JWT.StaticClaim(claim = "role", value = "admin"),
      @JWT.StaticClaim(claim = "aud", value = "${ENV}.kalix.io")
    })
  public static class ValueEntityWithServiceLevelJwt extends ValueEntity<User> {
    @PostMapping("/{userId}/{cartId}/create")
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }
  }

  @Id( {"userId", "cartId"})
  @TypeId("user")
  public static class ValueEntityWithMethodLevelJwt extends ValueEntity<User> {
    @PostMapping("/{userId}/{cartId}/create")
    @JWT(
      validate = JWT.JwtMethodMode.BEARER_TOKEN,
      bearerTokenIssuer = {"c", "d"},
      staticClaims = {
          @JWT.StaticClaim(claim = "role", value = "method-admin"),
          @JWT.StaticClaim(claim = "aud", value = "${ENV}")
      })
    public ValueEntity.Effect<Done> createEntity(@RequestBody CreateUser createUser) {
      return effects().reply(Done.instance);
    }
  }
}
