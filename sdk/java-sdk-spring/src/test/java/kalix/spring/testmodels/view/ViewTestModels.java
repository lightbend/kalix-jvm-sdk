/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.view;

import kalix.javasdk.annotations.*;
import kalix.javasdk.view.View;
import kalix.spring.testmodels.eventsourcedentity.Employee;
import kalix.spring.testmodels.eventsourcedentity.EmployeeEvent;
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EmployeeEntity;
import kalix.spring.testmodels.valueentity.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

public class ViewTestModels {

  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class) // when types are annotated, it's implicitly a transform = false
  public static class UserByEmailWithGet extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @GetMapping("/users/{email}")
    public User getUser(@PathVariable String email) {
      return null; // TODO: user should not implement this. we need to find a nice API for this
    }
  }

  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class) // when types are annotated, it's implicitly a transform = false
  public static class UserByEmailWithGetWithoutAnnotation extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @GetMapping("/users/{email}")
    public User getUser(String email) {
      return null;
    }

    @GetMapping("/users/{email}")
    public User getUserWithoutQuery(String email) {
      return null;
    }
  }


  @Subscribe.ValueEntity(UserEntity.class)
  public static class ViewWithoutTableAnnotation extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @GetMapping("/users/{email}")
    public User getUser(String email) {
      return null; // TODO: user should not implement this. we need to find a nice API for this
    }
  }

  @Table(" ")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class ViewWithoutEmptyTableAnnotation extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @GetMapping("/users/{email}")
    public User getUser(String email) {
      return null; // TODO: user should not implement this. we need to find a nice API for this
    }
  }

  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class UserByEmailWithPost extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public User getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table(value = "users_view")
  @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
  public static class UserByNameEmailWithPost extends View<User> {

    // mixing request body and path variable
    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/{name}/by-email")
    public User getUser(@PathVariable String name, @RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  public static class TransformedUserView extends View<TransformedUser> {

    // when methods are annotated, it's implicitly a transform = true
    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  public static class TransformedUserViewWithDeletes extends View<TransformedUser> {

    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
    public UpdateEffect<TransformedUser> onDelete() {
      return effects().deleteState();
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  public static class TransformedUserViewWithMethodLevelJWT extends View<TransformedUser> {

    // when methods are annotated, it's implicitly a transform = true
    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    @JWT(
        validate = JWT.JwtMethodMode.BEARER_TOKEN,
        bearerTokenIssuer = {"a", "b"},
        staticClaims = {
            @JWT.StaticClaim(claim = "role", value = "admin"),
            @JWT.StaticClaim(claim = "aud", value = "${ENV}.kalix.io")
        })
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  @JWT(
    validate = JWT.JwtMethodMode.BEARER_TOKEN,
    bearerTokenIssuer = {"a", "b"},
    staticClaims = {
        @JWT.StaticClaim(claim = "role", value = "admin"),
        @JWT.StaticClaim(claim = "aud", value = "${ENV}.kalix.io")
    })
  public static class ViewWithServiceLevelJWT extends View<User> {
    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public User getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }


  @Table("users_view")
  public static class TransformedUserViewUsingState extends View<TransformedUser> {

    // when methods are annotated, it's implicitly a transform = true
    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange(TransformedUser userView, User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  /**
   * This should be illegal. Either we subscribe at type level, and it's a transform = false. Or we
   * subscribe at method level, and it's a transform = true.
   */
  @Table("users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class ViewWithSubscriptionsInMixedLevels extends View<TransformedUser> {

    // when methods are annotated, it's implicitly a transform = true
    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  @Subscribe.ValueEntity(UserEntity.class) //it's implicitly a transform = false
  public static class TransformedViewWithoutSubscriptionOnMethodLevel extends View<TransformedUser> {

    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class ViewWithSubscriptionsInMixedLevelsHandleDelete extends View<User> {

    @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
    public UpdateEffect<User> onDelete() {
      return effects().deleteState();
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public User getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  public static class ViewWithoutSubscriptionButWithHandleDelete extends View<TransformedUser> {

    @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
    public UpdateEffect<TransformedUser> onDelete() {
      return effects().deleteState();
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  public static class ViewDuplicatedHandleDeletesAnnotations extends View<TransformedUser> {

    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
    public UpdateEffect<TransformedUser> onDelete() {
      return effects().deleteState();
    }

    @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
    public UpdateEffect<TransformedUser> onDelete2() {
      return effects().deleteState();
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  public static class ViewHandleDeletesWithParam extends View<TransformedUser> {

    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
    public UpdateEffect<TransformedUser> onDelete(User user) {
      return effects().deleteState();
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  public static class ViewWithHandleDeletesFalseOnMethodLevel extends View<TransformedUser> {

    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = false)
    public UpdateEffect<TransformedUser> onDelete() {
      return effects().deleteState();
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  public static class ViewDuplicatedVESubscriptions extends View<TransformedUser> {

    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
        .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange2(User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
    public UpdateEffect<TransformedUser> onDelete() {
      return effects().deleteState();
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  public static class ViewDuplicatedESSubscriptions extends View<TransformedUser> {

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
        .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public UpdateEffect<TransformedUser> onChange2(User user) {
      return effects()
        .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
    public UpdateEffect<TransformedUser> onDelete() {
      return effects().deleteState();
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table("users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class ViewWithNoQuery extends View<TransformedUser> {}

  @Table("users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class ViewWithTwoQueries extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public User getUserByEmail(@RequestBody ByEmail byEmail) {
      return null;
    }

    @Query("SELECT * FROM users_view WHERE email = :email AND name = :name")
    @PostMapping("/users/{name}/by-name")
    public User getUserByNameAndEmail(
        @PathVariable String name, @RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class UserByEmailWithPostRequestBodyOnly extends View<User> {

    // not path variables, only request body
    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public User getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class UserByNameStreamed extends View<User> {

    @Query("SELECT * FROM users_view WHERE name = :name")
    @GetMapping("/users/{name}")
    public Flux<User> getUser(@PathVariable String name) {
      return null;
    }
  }

  @Table(value = "employees_view")
  public static class SubscribeToEventSourcedEvents extends View<Employee> {

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public UpdateEffect<Employee> onCreated(EmployeeEvent.EmployeeCreated created) {
      return effects()
          .updateState(new Employee(created.firstName, created.lastName, created.email));
    }

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public UpdateEffect<Employee> onUpdated(EmployeeEvent.EmployeeEmailUpdated updated) {
      return effects().ignore();
    }

    @Query("SELECT * FROM employees_view WHERE email = :email")
    @PostMapping("/employees/by-email/{email}")
    public Employee getEmployeeByEmail(@PathVariable String email) {
      return null;
    }
  }

  @Table(value = "employees_view")
  public static class SubscribeToEventSourcedWithMissingHandlerState extends View<Employee> {

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public UpdateEffect<Employee> onCreated(Employee employee, EmployeeEvent.EmployeeCreated created) {
      return effects()
          .updateState(new Employee(created.firstName, created.lastName, created.email));
    }

    @Query("SELECT * FROM employees_view WHERE email = :email")
    @PostMapping("/employees/by-email/{email}")
    public Employee getEmployeeByEmail(@PathVariable String email) {
      return null;
    }
  }

  @Table(value = "employees_view")
  public static class SubscribeToEventSourcedEventsWithMethodWithState extends View<Employee> {

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public UpdateEffect<Employee> onCreated(Employee employee, EmployeeEvent.EmployeeCreated created) {
      return effects()
          .updateState(new Employee(created.firstName, created.lastName, created.email));
    }

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public UpdateEffect<Employee> onUpdated(EmployeeEvent.EmployeeEmailUpdated updated) {
      return effects().ignore();
    }

    @Query("SELECT * FROM employees_view WHERE email = :email")
    @PostMapping("/employees/by-email/{email}")
    public Employee getEmployeeByEmail(@PathVariable String email) {
      return null;
    }
  }

  @Table(value = "employees_view")
  @Subscribe.EventSourcedEntity(value = EmployeeEntity.class, ignoreUnknown = false)
  public static class TypeLevelSubscribeToEventSourcedEventsWithState extends View<Employee> {

    public UpdateEffect<Employee> onEvent(Employee employee, EmployeeEvent.EmployeeCreated created) {
      return effects()
          .updateState(new Employee(created.firstName, created.lastName, created.email));
    }

    @Query("SELECT * FROM employees_view WHERE email = :email")
    @PostMapping("/employees/by-email/{email}")
    public Employee getEmployeeByEmail(@PathVariable String email) {
      return null;
    }
  }

  @Table(value = "employees_view")
  @Acl(allow = @Acl.Matcher(service = "test"))
  public static class ViewWithServiceLevelAcl extends View<Employee> {
    @Query("SELECT * FROM employees_view WHERE email = :email")
    @PostMapping("/employees/by-email/{email}")
    public Employee getEmployeeByEmail(@PathVariable String email) {
      return null;
    }
  }

  @Table(value = "employees_view")
  public static class ViewWithMethodLevelAcl extends View<Employee> {
    @Query("SELECT * FROM employees_view WHERE email = :email")
    @PostMapping("/employees/by-email/{email}")
    @Acl(allow = @Acl.Matcher(service = "test"))
    public Employee getEmployeeByEmail(@PathVariable String email) {
      return null;
    }
  }

  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class UserByEmailWithStreamUpdates extends View<User> {

    @Query(value = "SELECT * FROM users_view WHERE email = :email", streamUpdates = true)
    @PostMapping("/users/by-email")
    public Flux<User> getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  @Table(value = "users_view_collection")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class UserByEmailWithCollectionReturn extends View<User> {

    @Query(value = "SELECT * AS users FROM users_view WHERE name = :name")
    @PostMapping("/users/by-name/{name}")
    public UserCollection getUser() {
      return null;
    }
  }

  @Table(value = "users_view")
  @Subscribe.ValueEntity(UserEntity.class)
  public static class IllDefineUserByEmailWithStreamUpdates extends View<User> {

    @Query(value = "SELECT * FROM users_view WHERE email = :email", streamUpdates = true)
    @PostMapping("/users/by-email")
    public User getUser(@RequestBody ByEmail byEmail) {
      return null;
    }
  }

  public static class MultiTableViewValidation {
    @Subscribe.ValueEntity(UserEntity.class)
    public static class ViewTableWithoutTableAnnotation extends View<User> {}

    @Table(" ")
    @Subscribe.ValueEntity(UserEntity.class)
    public static class ViewTableWithEmptyTableAnnotation extends View<User> {}

    @Table("users_view")
    @Subscribe.ValueEntity(UserEntity.class)
    public static class ViewTableWithMixedLevelSubscriptions extends View<TransformedUser> {
      @Subscribe.ValueEntity(UserEntity.class)
      public UpdateEffect<TransformedUser> onChange(User user) {
        return effects()
            .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
      }
    }
  }

  @ViewId("multi-table-view-without-query")
  public static class MultiTableViewWithoutQuery {
    public static class Users extends View<User> {}
  }

  @ViewId("multi-table-view-with-multiple-queries")
  public static class MultiTableViewWithMultipleQueries {
    @Query("SELECT * FROM users_view")
    @PostMapping("/users/query1")
    public User query1() {
      return null;
    }

    @Query("SELECT * FROM users_view")
    @PostMapping("/users/query2")
    public User query2() {
      return null;
    }

    @Table("users_view")
    public static class Users extends View<User> {}
  }

  @ViewId("multi-table-view-with-join-query")
  public static class MultiTableViewWithJoinQuery {
    @GetMapping("/employee-counters-by-email/{email}")
    @Query("""
      SELECT employees.*, counters.* as counters
      FROM employees
      JOIN assigned ON assigned.assigneeId = employees.email
      JOIN counters ON assigned.counterId = counters.id
      WHERE employees.email = :email
      """)
    public EmployeeCounters get(String email) {
      return null;
    }

    @Table("employees")
    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public static class Employees extends View<Employee> {
      public UpdateEffect<Employee> onCreated(EmployeeEvent.EmployeeCreated created) {
        return effects()
            .updateState(new Employee(created.firstName, created.lastName, created.email));
      }

      public UpdateEffect<Employee> onUpdated(EmployeeEvent.EmployeeEmailUpdated updated) {
        return effects().ignore();
      }
    }

    @Table("counters")
    @Subscribe.ValueEntity(Counter.class)
    public static class Counters extends View<CounterState> {}

    @Table("assigned")
    @Subscribe.ValueEntity(AssignedCounter.class)
    public static class Assigned extends View<AssignedCounterState> {}
  }

  @ViewId("multi-table-view-with-join-query")
  public static class MultiTableViewWithDuplicatedVESubscriptions {
    @Query("SELECT * FROM users_view")
    @PostMapping("/users/query1")
    public EmployeeCounters get(String email) {
      return null;
    }

    @Table("employees")
    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public static class Employees extends View<Employee> {
      public UpdateEffect<Employee> onEvent(EmployeeEvent.EmployeeCreated created) {
        return effects()
          .updateState(new Employee(created.firstName, created.lastName, created.email));
      }
    }

    @Table("counters")
    @Subscribe.ValueEntity(Counter.class)
    public static class Counters extends View<CounterState> {}

    @Table("assigned")
    public static class Assigned extends View<CounterState> {
      @Subscribe.ValueEntity(Counter.class)
      public UpdateEffect<CounterState> onEvent(CounterState counterState) {
        return effects().ignore();
      }

      @Subscribe.ValueEntity(Counter.class)
      public UpdateEffect<CounterState> onEvent2(CounterState counterState) {
        return effects().ignore();
      }
    }
  }

  @ViewId("multi-table-view-with-join-query")
  public static class MultiTableViewWithDuplicatedESSubscriptions {
    @Query("SELECT * FROM users_view")
    @PostMapping("/users/query1")
    public EmployeeCounters get(String email) {
      return null;
    }

    @Table("employees")
    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public static class Employees extends View<Employee> {
      public UpdateEffect<Employee> onEvent(EmployeeEvent.EmployeeCreated created) {
        return effects()
          .updateState(new Employee(created.firstName, created.lastName, created.email));
      }
    }

    @Table("counters")
    @Subscribe.ValueEntity(Counter.class)
    public static class Counters extends View<CounterState> {}

    @Table("assigned")
    public static class Assigned extends View<Employee> {
      @Subscribe.EventSourcedEntity(EmployeeEntity.class)
      public UpdateEffect<Employee> onEvent(CounterState counterState) {
        return effects().ignore();
      }

      @Subscribe.EventSourcedEntity(EmployeeEntity.class)
      public UpdateEffect<Employee> onEvent2(CounterState counterState) {
        return effects().ignore();
      }
    }
  }

  @ViewId("time-tracker-view")
  @Table("time-tracker-view")
  @Subscribe.ValueEntity(TimeTrackerEntity.class)
  public static class TimeTrackerView extends View<TimeTrackerEntity.TimerState> {

    @Query(value = "SELECT * FROM time-tracker-view WHERE name = :name")
    @PostMapping("/timer/query2")
    public TimeTrackerEntity.TimerState query2() {
      return null;
    }
  }


  @Table(value = "employee_table")
  @Subscribe.Topic(value = "source", consumerGroup = "cg")
  public static class TopicTypeLevelSubscriptionView extends View<Employee> {

    public UpdateEffect<Employee> onCreate(EmployeeEvent.EmployeeCreated evt) {
      return effects()
        .updateState(new Employee(evt.firstName, evt.lastName, evt.email));
    }

    public UpdateEffect<Employee> onEmailUpdate(EmployeeEvent.EmployeeEmailUpdated eeu) {
      var employee = viewState();
      return effects().updateState(new Employee(employee.firstName, employee.lastName, eeu.email));
    }

    @Query("SELECT * FROM employees_view WHERE email = :email")
    @PostMapping("/employees/by-email/{email}")
    public Employee getEmployeeByEmail(@PathVariable String email) {
      return null;
    }
  }

  @Table(value = "employee_table")
  public static class TopicSubscriptionView extends View<Employee> {

    @Subscribe.Topic(value = "source", consumerGroup = "cg")
    public UpdateEffect<Employee> onCreate(EmployeeEvent.EmployeeCreated evt) {
      return effects()
        .updateState(new Employee(evt.firstName, evt.lastName, evt.email));
    }

    @Subscribe.Topic(value = "source", consumerGroup = "cg")
    public UpdateEffect<Employee> onEmailUpdate(EmployeeEvent.EmployeeEmailUpdated eeu) {
      var employee = viewState();
      return effects().updateState(new Employee(employee.firstName, employee.lastName, eeu.email));
    }

    @Query("SELECT * FROM employees_view WHERE email = :email")
    @PostMapping("/employees/by-email/{email}")
    public Employee getEmployeeByEmail(@PathVariable String email) {
      return null;
    }
  }
}
