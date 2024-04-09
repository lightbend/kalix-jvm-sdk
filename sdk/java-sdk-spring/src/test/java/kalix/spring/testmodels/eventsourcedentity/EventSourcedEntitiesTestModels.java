/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.eventsourcedentity;

import kalix.javasdk.JsonMigration;
import kalix.javasdk.annotations.*;
import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public class EventSourcedEntitiesTestModels {

  @Id("id")
  @TypeId("employee")
  @RequestMapping("/employee/{id}")
  public static class EmployeeEntity extends EventSourcedEntity<Employee, EmployeeEvent> {

    @PostMapping
    public Effect<String> createUser(@RequestBody CreateEmployee create) {
      return effects()
          .emitEvent(new EmployeeEvent.EmployeeCreated(create.firstName, create.lastName, create.email))
          .thenReply(__ -> "ok");
    }

    @EventHandler
    public Employee onEvent(EmployeeEvent event) {
      EmployeeEvent.EmployeeCreated create = (EmployeeEvent.EmployeeCreated) event;
      return new Employee(create.firstName, create.lastName, create.email);
    }
  }

  @Id("id")
  @TypeId("counter-entity")
  @RequestMapping("/eventsourced/{id}")
  public static class CounterEventSourcedEntity extends EventSourcedEntity<Integer, Object> {

    @Migration(EventMigration.class)
    public record Event(String s) {
    }

    public static class EventMigration extends JsonMigration {

      public EventMigration() {
      }

      @Override
      public int currentVersion() {
        return 1;
      }

      @Override
      public List<String> supportedClassNames() {
        return List.of("additional-mapping");
      }
    }

    @GetMapping("/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }

    @PostMapping("/changeInt/{number}")
    public Integer changeInteger(@PathVariable Integer number) {
      return number;
    }

    @EventHandler
    public Integer receiveStringEvent(Event event) {
      return 0;
    }

    @EventHandler
    public Integer receivedIntegerEvent(Integer event) {
      return 0;
    }

    public Integer publicMethodSimilarSignature(Integer event) {
      return 0;
    }

    private Integer privateMethodSimilarSignature(Integer event) {
      return 0;
    }
  }


  @TypeId("counter")
  public static class CounterEventSourcedEntityWithIdOnMethod extends EventSourcedEntity<Integer, Object> {
    @Id("id")
    @GetMapping("/eventsourced/{id}/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @TypeId("counter")
  public static class ESEntityCompoundIdIncorrectOrder extends EventSourcedEntity<Integer, Object> {
    @Id({"id", "id2"})
    @GetMapping("/{id2}/eventsourced/{id}/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @Id("id")
  @TypeId("counter")
  public static class CounterEventSourcedEntityWithIdMethodOverride extends EventSourcedEntity<Integer, Object> {

    @Id("counter_id")
    @GetMapping("/eventsourced/{counter_id}/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @TypeId("counter")
  public static class CounterEventSourcedEntityWithIdGenerator extends EventSourcedEntity<Integer, Object> {
    @GenerateId
    @PutMapping("/eventsourced/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @TypeId("counter")
  public static class IllDefinedEntityWithIdGeneratorAndId extends EventSourcedEntity<Integer, Object> {
    @GenerateId
    @Id("id")
    @GetMapping("/eventsourced/{id}/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @TypeId("counter")
  public static class IllDefinedEntityWithoutIdGeneratorNorId extends EventSourcedEntity<Integer, Object> {
    @GetMapping("/eventsourced/{id}/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @Id("id")
  @TypeId("counter")
  @RequestMapping("/eventsourced/{id}")
  public static class CounterEventSourcedEntityWithMethodLevelJWT extends EventSourcedEntity<Integer, Object> {

    @GetMapping("/int/{number}")
    @JWT(
        validate = JWT.JwtMethodMode.BEARER_TOKEN,
        bearerTokenIssuer = {"a", "b"})
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }

    @PostMapping("/changeInt/{number}")
    @JWT(
        validate = JWT.JwtMethodMode.BEARER_TOKEN,
        bearerTokenIssuer = {"c", "d"},
        staticClaims = {
            @JWT.StaticClaim(claim = "role", value = "method-admin"),
            @JWT.StaticClaim(claim = "aud", value = "${ENV}")
        })
    public Integer changeInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @Id("id")
  @TypeId("counter")
  @RequestMapping("/eventsourced/{id}")
  @JWT(
    validate = JWT.JwtMethodMode.BEARER_TOKEN,
    bearerTokenIssuer = {"a", "b"},
    staticClaims = {
        @JWT.StaticClaim(claim = "role", value = "admin"),
        @JWT.StaticClaim(claim = "aud", value = "${ENV}.kalix.io")
    })
  public static class CounterEventSourcedEntityWithServiceLevelJWT extends EventSourcedEntity<Integer, Object> {

    @GetMapping("/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }

    @PostMapping("/changeInt/{number}")
    public Integer changeInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @Id("id")
  @TypeId("counter")
  public static class ErrorDuplicatedEventsEntity extends EventSourcedEntity<Integer, Object> {

    @EventHandler
    public Integer receiveStringEvent(String event) {
      return 0;
    }

    @EventHandler
    public Integer receivedIntegerEvent(Integer event) {
      return 0;
    }

    @EventHandler
    public Integer receivedIntegerEventDup(Integer event) {
      return 0;
    }
  }

  @Id("id")
  @TypeId("counter")
  public static class ErrorWrongSignaturesEntity extends EventSourcedEntity<Integer, Object> {

    @EventHandler
    public String receivedIntegerEvent(Integer event) {
      return "0";
    }

    @EventHandler
    public Integer receivedIntegerEventAndString(Integer event, String s1) {
      return 0;
    }
  }

  @Id("id")
  @TypeId("employee")
  @RequestMapping("/employee/{id}")
  public static class EmployeeEntityWithMissingHandler extends EventSourcedEntity<Employee, EmployeeEvent> {

    @PostMapping
    public Effect<String> createUser(@RequestBody CreateEmployee create) {
      return effects()
          .emitEvent(new EmployeeEvent.EmployeeCreated(create.firstName, create.lastName, create.email))
          .thenReply(__ -> "ok");
    }

    @EventHandler
    public Employee onEvent(EmployeeEvent.EmployeeCreated created) {
      return new Employee(created.firstName, created.lastName, created.email);
    }
  }

  @Id("id")
  @TypeId("employee")
  @RequestMapping("/employee/{id}")
  public static class EmployeeEntityWithMixedHandlers extends EventSourcedEntity<Employee, EmployeeEvent> {

    @PostMapping
    public Effect<String> createUser(@RequestBody CreateEmployee create) {
      return effects()
          .emitEvent(new EmployeeEvent.EmployeeCreated(create.firstName, create.lastName, create.email))
          .thenReply(__ -> "ok");
    }

    @EventHandler
    public Employee onEvent(EmployeeEvent event) {
      if (event instanceof EmployeeEvent.EmployeeCreated) {
        EmployeeEvent.EmployeeCreated created = (EmployeeEvent.EmployeeCreated) event;
        return new Employee(created.firstName, created.lastName, created.email);
      } else {
        return currentState();
      }
    }

    @EventHandler
    public Employee onEmployeeCreated(EmployeeEvent.EmployeeCreated created) {
      return new Employee(created.firstName, created.lastName, created.email);
    }
  }

  @Id("id")
  @TypeId("counter")
  @Acl(allow = @Acl.Matcher(service = "test"))
  public static class EventSourcedEntityWithServiceLevelAcl extends EventSourcedEntity<Integer, Object> {

  }


  @Id("id")
  @TypeId("counter")
  @RequestMapping("/employee/{id}")
  public static class EventSourcedEntityWithMethodLevelAcl extends EventSourcedEntity<Integer, Object> {
    @PostMapping
    @Acl(allow = @Acl.Matcher(service = "test"))
    public Effect<String> createUser(@RequestBody CreateEmployee create) {
      return effects()
          .emitEvent(new EmployeeEvent.EmployeeCreated(create.firstName, create.lastName, create.email))
          .thenReply(__ -> "ok");
    }
  }
}
