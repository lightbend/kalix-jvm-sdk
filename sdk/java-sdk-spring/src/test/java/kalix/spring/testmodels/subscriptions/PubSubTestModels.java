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

package kalix.spring.testmodels.subscriptions;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Acl;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Subscribe;
import kalix.spring.testmodels.Message;
import kalix.spring.testmodels.Message2;
import kalix.spring.testmodels.valueentity.Counter;
import kalix.spring.testmodels.valueentity.CounterState;
import kalix.javasdk.view.View;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Table;
import kalix.spring.testmodels.eventsourcedentity.Employee;
import kalix.spring.testmodels.eventsourcedentity.EmployeeEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntity;
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EmployeeEntity;

public class PubSubTestModels {//TODO shall we remove this class and move things to ActionTestModels and ViewTestModels

  public static class SubscribeToValueEntityAction extends Action {

    @Subscribe.ValueEntity(Counter.class)
    public Action.Effect<CounterState> onUpdate(CounterState message) {
      return effects().reply(message);
    }
  }

  public static class SubscribeToValueEntityWithDeletesAction extends Action {

    @Subscribe.ValueEntity(Counter.class)
    public Action.Effect<CounterState> onUpdate(CounterState message) {
      return effects().reply(message);
    }

    @Subscribe.ValueEntity(value = Counter.class, handleDeletes = true)
    public Action.Effect<CounterState> onDelete() {
      return effects().ignore();
    }
  }

  public static class SubscribeToEventSourcedEntityAction extends Action {

    @Subscribe.EventSourcedEntity(CounterEventSourcedEntity.class)
    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    @Subscribe.EventSourcedEntity(CounterEventSourcedEntity.class)
    public Action.Effect<String> methodTwo(String message) {
      return effects().reply(message);
    }
  }

  @Subscribe.Topic(value = "topicAAA", consumerGroup = "aa")
  public static class SubscribeToTopicActionTypeLevelMethodLevel extends Action {

    public Action.Effect<Message> messageOne(Message message) {return effects().reply(message);}

    @Subscribe.Topic(value = "topicXYZ")
    public Action.Effect<Message2> messageTwo(Message2 message) {return effects().reply(message);}
  }

  @Subscribe.Topic(value = "topicXYZ", ignoreUnknown = true)
  public static class SubscribeToTopicsActionTypeLevel extends Action {

    public Action.Effect<Message> methodOne(Message message) {
      return effects().reply(message);
    }

    public Action.Effect<Message2> methodTwo(Message2 message) {
      return effects().reply(message);
    }
  }

  public static class RestAnnotatedSubscribeToEventSourcedEntityAction extends Action {
    @PostMapping("/changeInt/{number}")
    @Subscribe.EventSourcedEntity(CounterEventSourcedEntity.class)
    public Action.Effect<Integer> methodTwo(Integer number) {
      return effects().reply(number);
    }
  }

  @Subscribe.EventSourcedEntity(value = CounterEventSourcedEntity.class, ignoreUnknown = true)
  public static class SubscribeOnlyOneToEventSourcedEntityActionTypeLevel extends Action {

    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }
  }

  @Subscribe.EventSourcedEntity(value = CounterEventSourcedEntity.class, ignoreUnknown = true)
  public static class InvalidSubscribeToEventSourcedEntityAction extends Action {

    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public Action.Effect<String> methodTwo(String message) {
      return effects().reply(message);
    }
  }

  public static class SubscribeToTopicAction extends Action {

    @Subscribe.Topic(value = "topicXYZ", consumerGroup = "cg")
    public Action.Effect<Message> messageOne(Message message) {
      return effects().reply(message);
    }
  }

  @Subscribe.Topic(value = "topicXYZ", consumerGroup = "cg")
  public static class InvalidSubscribeToTopicAction extends Action {

    @Subscribe.Topic(value = "topicXYZ", consumerGroup = "cg")
    public Action.Effect<Message> messageOne(Message message) {
      return effects().reply(message);
    }
  }

  public static class SubscribeToTwoTopicsAction extends Action {

    @Subscribe.Topic("topicXYZ")
    public Action.Effect<Message> methodOne(Message message) {
      return effects().reply(message);
    }

    @Subscribe.Topic("topicXYZ")
    public Action.Effect<Message2> methodTwo(Message2 message) {
      return effects().reply(message);
    }

    @Subscribe.Topic("topicXYZ")
    public Action.Effect<Integer> methodThree(Integer message) {
      return effects().reply(message);
    }
  }

  public static class PublishToTopicAction extends Action {

    @Publish.Topic("topicAlpha")
    public Action.Effect<Message> messageOne(Message message) {
      return effects().reply(message);
    }
  }

  public static class RestWithPublishToTopicAction extends Action {

    @PostMapping("/message/{msg}")
    @Publish.Topic("foobar")
    public Effect<Message> messageOne(@PathVariable String msg) {
      return effects().reply(new Message(msg));
    }
  }

  public static class RestAnnotatedSubscribeToValueEntityAction extends Action {
    // this should fail as not allowed
    @Subscribe.ValueEntity(Counter.class)
    @PostMapping("/message/one")
    public Action.Effect<Message> messageOne(@RequestBody Message message) {
      return effects().reply(message);
    }
  }


  @Acl(allow = @Acl.Matcher(service = "test"))
  public static class ActionWithServiceLevelAcl extends Action {
  }


  public static class ActionWithMethodLevelAcl extends Action {
    @Acl(allow = @Acl.Matcher(service = "test"))
    @PostMapping("/message/one")
    public Action.Effect<Message> messageOne(@RequestBody Message message) {
      return effects().reply(message);
    }
  }

  public static class ActionWithMethodLevelAclAndSubscription extends Action {
    @Acl(allow = @Acl.Matcher(service = "test"))
    @Subscribe.ValueEntity(Counter.class)
    public Action.Effect<Message> messageOne(@RequestBody Message message) {
      return effects().reply(message);
    }
  }

  @Table(value = "employee_table")
  @Subscribe.EventSourcedEntity(value = EmployeeEntity.class, ignoreUnknown = true)
  public static class SubscribeOnTypeToEventSourcedEvents extends View<Employee> {

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


    @Subscribe.EventSourcedEntity(value = EmployeeEntity.class)
    @Publish.Stream(id = "employee_events")
    public static class EventStreamPublishingAction extends Action {

      public Effect<String> transform(EmployeeEvent.EmployeeCreated created) {
        return effects().reply(created.toString());
      }

      public Effect<String> transform(EmployeeEvent.EmployeeEmailUpdated emailUpdated) {
        return effects().reply(emailUpdated.toString());
      }

    }

    @Subscribe.Stream(service = "employee_service", id = "employee_events", ignoreUnknown = true)
    public static class EventStreamSubscriptionAction extends Action {

      public Effect<String> transform(EmployeeEvent.EmployeeCreated created) {
        return effects().reply(created.toString());
      }

      public Effect<String> transform(EmployeeEvent.EmployeeEmailUpdated emailUpdated) {
        return effects().reply(emailUpdated.toString());
      }
    }

    @Table(value = "employee_table")
    @Subscribe.Stream(service = "employee_service", id = "employee_events")
    public static class EventStreamSubscriptionView extends View<Employee> {

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
  }
