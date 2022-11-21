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

package kalix.springsdk.testmodels.view;

import kalix.javasdk.view.View;
import kalix.springsdk.annotations.*;
import kalix.springsdk.testmodels.eventsourcedentity.Employee;
import kalix.springsdk.testmodels.eventsourcedentity.EmployeeEvent;
import kalix.springsdk.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels;
import kalix.springsdk.testmodels.valueentity.Counter;
import kalix.springsdk.testmodels.valueentity.User;
import kalix.springsdk.testmodels.valueentity.UserEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

public class ViewTestModels {

  @Table(value = "users_view")
  @Subscribe.ValueEntity(
      UserEntity.class) // when types are annotated, it's implicitly a transform = false
  public static class UserByEmailWithGet extends View<User> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @GetMapping("/users/{email}")
    public User getUser(String email) {
      return null; // TODO: user should not implement this. we need to find a nice API for this
    }

    @Override
    public User emptyState() {
      return null; // TODO: user should not have to implement this when not transforming
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
  public static class TransformedUserViewWithJWT extends View<TransformedUser> {

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
        sign = JWT.JwtMethodMode.MESSAGE,
        bearerTokenIssuer = {"a", "b"})
    public TransformedUser getUser(@RequestBody ByEmail byEmail) {
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
  public static class ViewDuplicatedSubscriptions extends View<TransformedUser> {

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
  public static class ViewWithMissingSubscriptionForHandleDeletes extends View<TransformedUser> {

    @Subscribe.ValueEntity(UserEntity.class)
    public UpdateEffect<TransformedUser> onChange(User user) {
      return effects()
          .updateState(new TransformedUser(user.lastName + ", " + user.firstName, user.email));
    }

    @Subscribe.ValueEntity(value = UserEntity.class, handleDeletes = true)
    public UpdateEffect<TransformedUser> onUserDelete() {
      return effects().deleteState();
    }

    @Subscribe.ValueEntity(value = Counter.class, handleDeletes = true)
    public UpdateEffect<TransformedUser> onCounterDelete() {
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
  public static class ViewWithTwoQueries extends View<TransformedUser> {

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/by-email")
    public TransformedUser getUserByEmail(@RequestBody ByEmail byEmail) {
      return null;
    }

    @Query("SELECT * FROM users_view WHERE email = :email")
    @PostMapping("/users/{name}/by-name")
    public TransformedUser getUserByNameAndEmail(
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

    @Subscribe.EventSourcedEntity(EventSourcedEntitiesTestModels.EmployeeEntity.class)
    public UpdateEffect<Employee> onEvent(EmployeeEvent evt) {
      EmployeeEvent.EmployeeCreated created = (EmployeeEvent.EmployeeCreated) evt;
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

    @Subscribe.EventSourcedEntity(EventSourcedEntitiesTestModels.EmployeeEntity.class)
    public UpdateEffect<Employee> onEvent(Employee employee, EmployeeEvent evt) {
      EmployeeEvent.EmployeeCreated created = (EmployeeEvent.EmployeeCreated) evt;
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

}
