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

package kalix.springsdk.testmodels.eventsourcedentity;

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.springsdk.annotations.*;
import org.springframework.web.bind.annotation.*;

public class EventSourcedEntitiesTestModels {

  @EntityKey("id")
  @EntityType("employee")
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

  @EntityKey("id")
  @EntityType("counter")
  @RequestMapping("/eventsourced/{id}")
  public static class CounterEventSourcedEntity extends EventSourcedEntity<Integer, Object> {

    @GetMapping("/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }

    @PostMapping("/changeInt/{number}")
    public Integer changeInteger(@PathVariable Integer number) {
      return number;
    }

    @EventHandler
    public Integer receiveStringEvent(String event) {
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


  @EntityType("counter")
  public static class CounterEventSourcedEntityWithEntityKeyOnMethod extends EventSourcedEntity<Integer, Object> {
    @EntityKey("id")
    @GetMapping("/eventsourced/{id}/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @EntityKey("id")
  @EntityType("counter")
  public static class CounterEventSourcedEntityWithEntityKeyMethodOverride extends EventSourcedEntity<Integer, Object> {

    @EntityKey("counter_id")
    @GetMapping("/eventsourced/{counter_id}/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @EntityType("counter")
  public static class CounterEventSourcedEntityWithEntityKeyGenerator extends EventSourcedEntity<Integer, Object> {
    @GenerateEntityKey
    @PutMapping("/eventsourced/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @EntityType("counter")
  public static class IllDefinedEntityWithEntityKeyGeneratorAndEntityKey extends EventSourcedEntity<Integer, Object> {
    @GenerateEntityKey
    @EntityKey("id")
    @GetMapping("/eventsourced/{id}/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @EntityType("counter")
  public static class IllDefinedEntityWithoutEntityKeyGeneratorNorEntityKey extends EventSourcedEntity<Integer, Object> {
    @GetMapping("/eventsourced/{id}/int/{number}")
    public Integer getInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @EntityKey("id")
  @EntityType("counter")
  @RequestMapping("/eventsourced/{id}")
  public static class CounterEventSourcedEntityWithJWT extends EventSourcedEntity<Integer, Object> {

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
        bearerTokenIssuer = {"a", "b"})
    public Integer changeInteger(@PathVariable Integer number) {
      return number;
    }
  }

  @EntityKey("id")
  @EntityType("counter")
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

  @EntityKey("id")
  @EntityType("counter")
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

  @EntityKey("id")
  @EntityType("counter")
  @Acl(allow = @Acl.Matcher(service = "test"))
  public static class EventSourcedEntityWithServiceLevelAcl extends EventSourcedEntity<Integer, Object> {

  }


  @EntityKey("id")
  @EntityType("counter")
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
