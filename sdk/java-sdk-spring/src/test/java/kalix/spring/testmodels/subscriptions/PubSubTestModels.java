/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.spring.testmodels.subscriptions;

import kalix.javasdk.action.Action;
import kalix.javasdk.annotations.Acl;
import kalix.javasdk.annotations.Publish;
import kalix.javasdk.annotations.Query;
import kalix.javasdk.annotations.Subscribe;
import kalix.javasdk.annotations.Table;
import kalix.javasdk.view.View;
import kalix.spring.testmodels.Done;
import kalix.spring.testmodels.Message;
import kalix.spring.testmodels.Message2;
import kalix.spring.testmodels.eventsourcedentity.Employee;
import kalix.spring.testmodels.eventsourcedentity.EmployeeEvent.EmployeeCreated;
import kalix.spring.testmodels.eventsourcedentity.EmployeeEvent.EmployeeEmailUpdated;
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.CounterEventSourcedEntity;
import kalix.spring.testmodels.eventsourcedentity.EventSourcedEntitiesTestModels.EmployeeEntity;
import kalix.spring.testmodels.valueentity.AssignedCounter;
import kalix.spring.testmodels.valueentity.Counter;
import kalix.spring.testmodels.valueentity.CounterState;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public class PubSubTestModels {//TODO shall we remove this class and move things to ActionTestModels and ViewTestModels

  public static class SubscribeToValueEntityAction extends Action {

    @Subscribe.ValueEntity(Counter.class)
    public Action.Effect<CounterState> onUpdate(CounterState message) {
      return effects().reply(message);
    }
  }

  @Subscribe.ValueEntity(Counter.class)
  public static class SubscribeToValueEntityTypeLevelAction extends Action {

    public Action.Effect<CounterState> onUpdate(CounterState message) {
      return effects().reply(message);
    }
  }

  public static class SubscribeToValueEntityWithRestAction extends Action {

    @Subscribe.ValueEntity(Counter.class)
    public Action.Effect<CounterState> onUpdate(CounterState message) {
      return effects().reply(message);
    }

    @GetMapping("/test")
    public Effect<String> get() {
      return effects().reply("test");
    }
  }

  @Subscribe.ValueEntity(Counter.class)
  public static class TypeLevelSubscribeToValueEntityWithRestAction extends Action {

    public Action.Effect<CounterState> onUpdate(CounterState message) {
      return effects().reply(message);
    }

    @GetMapping("/test")
    public Effect<String> get() {
      return effects().reply("test");
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

  @Subscribe.EventSourcedEntity(EmployeeEntity.class)
  public static class SubscribeToEventSourcedEmployee extends Action {

    public Effect<EmployeeCreated> methodOne(EmployeeCreated message) {
      return effects().reply(message);
    }

    public Effect<EmployeeEmailUpdated> methodTwo(EmployeeEmailUpdated message) {
      return effects().reply(message);
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

  @Subscribe.ValueEntity(Counter.class)
  @Subscribe.EventSourcedEntity(EmployeeEntity.class)
  @Subscribe.Topic("topic")
  @Subscribe.Stream(id = "source", service = "abc")
  public static class MultipleTypeLevelSubscriptionsInAction extends Action {

    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

  }

  @Subscribe.ValueEntity(Counter.class)
  public static class MultipleUpdateMethodsForVETypeLevelSubscriptionInAction extends Action {

    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    public Action.Effect<String> methodTwo(String message) {
      return effects().reply(message);
    }
  }

  public static class AmbiguousHandlersVESubscriptionInAction extends Action {

    @Subscribe.ValueEntity(Counter.class)
    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    @Subscribe.ValueEntity(Counter.class)
    public Action.Effect<Integer> methodTwo(Integer message) {
      return effects().reply(message);
    }

    @Subscribe.ValueEntity(AssignedCounter.class)
    public Action.Effect<Integer> methodThree(Integer message) {
      return effects().reply(message);
    }
  }

  public static class AmbiguousDeleteHandlersVESubscriptionInAction extends Action {

    @Subscribe.ValueEntity(value = Counter.class, handleDeletes = true)
    public Action.Effect<Integer> methodOne() {
      return effects().ignore();
    }

    @Subscribe.ValueEntity(value = Counter.class, handleDeletes = true)
    public Action.Effect<Integer> methodTwo() {
      return effects().ignore();
    }

    @Subscribe.ValueEntity(value = AssignedCounter.class, handleDeletes = true)
    public Action.Effect<Integer> methodThree() {
      return effects().ignore();
    }
  }

  @Subscribe.ValueEntity(Counter.class)
  public static class AmbiguousHandlersVETypeLevelSubscriptionInAction extends Action {

    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    public Action.Effect<Integer> methodTwo(Integer message) {
      return effects().reply(message);
    }
  }

  public static class AmbiguousHandlersESSubscriptionInAction extends Action {

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public Action.Effect<Integer> methodTwo(Integer message) {
      return effects().reply(message);
    }

    @Subscribe.EventSourcedEntity(CounterEventSourcedEntity.class)
    public Action.Effect<Integer> methodThree(Integer message) {
      return effects().reply(message);
    }
  }

  @Subscribe.EventSourcedEntity(EmployeeEntity.class)
  public static class AmbiguousHandlersESTypeLevelSubscriptionInAction extends Action {

    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    public Action.Effect<Integer> methodTwo(Integer message) {
      return effects().reply(message);
    }
  }

  @Subscribe.Stream(id = "source", service = "a")
  public static class AmbiguousHandlersStreamTypeLevelSubscriptionInAction extends Action {

    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    public Action.Effect<Integer> methodTwo(Integer message) {
      return effects().reply(message);
    }
  }

  public static class AmbiguousHandlersTopiSubscriptionInAction extends Action {

    @Subscribe.Topic("source")
    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    @Subscribe.Topic("source")
    public Action.Effect<Integer> methodTwo(Integer message) {
      return effects().reply(message);
    }

    @Subscribe.Topic("source-2")
    public Action.Effect<Integer> methodThree(Integer message) {
      return effects().reply(message);
    }
  }

  @Subscribe.Topic("source")
  public static class AmbiguousHandlersTopicTypeLevelSubscriptionInAction extends Action {

    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    public Action.Effect<Integer> methodTwo(Integer message) {
      return effects().reply(message);
    }
  }

  public static class MissingSourceForTopicPublishing extends Action {

    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }
  }

  public static class MissingTopicForVESubscription extends Action {

    @Subscribe.ValueEntity(Counter.class)
    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    @Subscribe.ValueEntity(value = Counter.class, handleDeletes = true)
    public Action.Effect<String> methodTwo() {
      return effects().ignore();
    }
  }

  public static class MissingTopicForESSubscription extends Action {

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    public Action.Effect<String> methodTwo(Integer message) {
      return effects().ignore();
    }
  }

  @Subscribe.EventSourcedEntity(EmployeeEntity.class)
  public static class MissingTopicForTypeLevelESSubscription extends Action {

    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    public Action.Effect<String> methodTwo(Integer message) {
      return effects().ignore();
    }
  }

  public static class MissingTopicForTopicSubscription extends Action {

    @Subscribe.Topic("source")
    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    @Subscribe.Topic("source")
    public Action.Effect<String> methodTwo(Integer message) {
      return effects().ignore();
    }
  }

  @Subscribe.Topic("source")
  public static class MissingTopicForTopicTypeLevelSubscription extends Action {

    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    public Action.Effect<String> methodTwo(Integer message) {
      return effects().ignore();
    }
  }

  @Subscribe.Stream(id = "source", service = "abc")
  public static class MissingTopicForStreamSubscription extends Action {

    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    public Action.Effect<String> methodTwo(Integer message) {
      return effects().ignore();
    }
  }

  public static class DifferentTopicForVESubscription extends Action {

    @Subscribe.ValueEntity(Counter.class)
    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    @Subscribe.ValueEntity(value = Counter.class, handleDeletes = true)
    @Publish.Topic("another-topic")
    public Action.Effect<String> methodTwo() {
      return effects().ignore();
    }
  }

  public static class DifferentTopicForESSubscription extends Action {

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    @Publish.Topic("another-topic")
    public Action.Effect<String> methodTwo(Integer message) {
      return effects().ignore();
    }
  }

  @Subscribe.EventSourcedEntity(EmployeeEntity.class)
  public static class DifferentTopicForESTypeLevelSubscription extends Action {

    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    @Publish.Topic("another-topic")
    public Action.Effect<String> methodTwo(Integer message) {
      return effects().ignore();
    }
  }

  public static class DifferentTopicForTopicSubscription extends Action {

    @Subscribe.Topic("source")
    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    @Subscribe.Topic("source")
    @Publish.Topic("another-topic")
    public Action.Effect<String> methodTwo(Integer message) {
      return effects().ignore();
    }
  }

  @Subscribe.Topic("source")
  public static class DifferentTopicForTopicTypeLevelSubscription extends Action {

    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    @Publish.Topic("another-topic")
    public Action.Effect<String> methodTwo(Integer message) {
      return effects().ignore();
    }
  }

  @Subscribe.Stream(id = "source", service = "abc")
  public static class DifferentTopicForStreamSubscription extends Action {

    @Publish.Topic("test")
    public Action.Effect<String> methodOne(String message) {
      return effects().reply(message);
    }

    @Publish.Topic("another-topic")
    public Action.Effect<String> methodTwo(Integer message) {
      return effects().ignore();
    }
  }

  @Subscribe.EventSourcedEntity(value = EmployeeEntity.class)
  public static class MissingHandlersWhenSubscribeToEventSourcedEntityAction extends Action {

    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    public Action.Effect<String> onEvent(EmployeeCreated message) {
      return effects().reply(message.toString());
    }
  }

  public static class MissingHandlersWhenSubscribeToEventSourcedOnMethodLevelEntityAction extends Action {

    public Action.Effect<Integer> methodOne(Integer message) {
      return effects().reply(message);
    }

    @Subscribe.EventSourcedEntity(value = EmployeeEntity.class)
    public Action.Effect<String> onEvent(EmployeeCreated message) {
      return effects().reply(message.toString());
    }
  }

  public static class SubscribeToTopicAction extends Action {

    @Subscribe.Topic(value = "topicXYZ", consumerGroup = "cg")
    public Action.Effect<Message> messageOne(Message message) {
      return effects().reply(message);
    }
  }

  @Subscribe.Topic(value = "topicXYZ", consumerGroup = "cg")
  public static class SubscribeToTopicTypeLevelAction extends Action {

    public Action.Effect<Message> messageOne(Message message) {
      return effects().reply(message);
    }
  }

  @Subscribe.Topic(value = "topicXYZ", consumerGroup = "cg", ignoreUnknown = true)
  public static class SubscribeToTopicTypeLevelCombinedAction extends Action {

    public Action.Effect<Message> messageOne(Message message) {
      return effects().reply(message);
    }

    public Action.Effect<String> messageTwo(String message) {
      return effects().reply(message);
    }
  }

  public static class SubscribeToTopicCombinedAction extends Action {

    @Subscribe.Topic(value = "topicXYZ", consumerGroup = "cg")
    public Action.Effect<Message> messageOne(Message message) {
      return effects().reply(message);
    }

    @Subscribe.Topic(value = "topicXYZ", consumerGroup = "cg")
    public Action.Effect<String> messageTwo(String message) {
      return effects().reply(message);
    }
  }

  public static class InvalidConsumerGroupsWhenSubscribingToTopicAction extends Action {

    @Subscribe.Topic(value = "topicXYZ", consumerGroup = "cg")
    public Action.Effect<Message> messageOne(Message message) {
      return effects().reply(message);
    }

    @Subscribe.Topic(value = "topicXYZ", consumerGroup = "cg2")
    public Action.Effect<String> messageTwo(String message) {
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

  public static class RestWithPublishToTopicAction extends Action {

    @PostMapping("/message/{msg}")
    @Publish.Topic("foobar")
    public Effect<Message> messageOne(@PathVariable String msg) {
      return effects().reply(new Message(msg));
    }
  }

  @Subscribe.ValueEntity(Counter.class)
  public static class PublishBytesToTopicAction extends Action {

    @Publish.Topic("foobar")
    public Effect<byte[]> produce(Message msg) {
      return effects().reply(msg.value().getBytes());
    }
  }

  public static class SubscribeToBytesFromTopicAction extends Action {

    @Subscribe.Topic("foobar")
    public Effect<Done> consume(byte[] bytes) {
      return effects().reply(Done.instance);
    }
  }

  public static class VEWithPublishToTopicAction extends Action {

    @Subscribe.ValueEntity(Counter.class)
    @Publish.Topic("foobar")
    public Effect<Message> messageOne(String msg) {
      return effects().reply(new Message(msg));
    }

    @Subscribe.ValueEntity(value = Counter.class, handleDeletes = true)
    @Publish.Topic("foobar")
    public Effect<Message> messageTwo() {
      return effects().ignore();
    }
  }

  public static class ESWithPublishToTopicAction extends Action {

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    @Publish.Topic("foobar")
    public Effect<Message> messageOne(EmployeeCreated created) {
      return effects().reply(new Message(created.firstName));
    }

    @Subscribe.EventSourcedEntity(EmployeeEntity.class)
    @Publish.Topic("foobar")
    public Effect<Message> messageTwo(EmployeeEmailUpdated updated) {
      return effects().ignore();
    }
  }

  @Subscribe.EventSourcedEntity(EmployeeEntity.class)
  public static class TypeLevelESWithPublishToTopicAction extends Action {

    @Publish.Topic("foobar")
    public Effect<Message> messageOne(EmployeeCreated created) {
      return effects().reply(new Message(created.firstName));
    }

    @Publish.Topic("foobar")
    public Effect<Message> messageTwo(EmployeeEmailUpdated updated) {
      return effects().ignore();
    }
  }

  @Subscribe.Topic("source")
  public static class TypeLevelTopicSubscriptionWithPublishToTopicAction extends Action {

    @Publish.Topic("foobar")
    public Effect<Message> messageOne(String msg) {
      return effects().reply(new Message(msg));
    }

    @Publish.Topic("foobar")
    public Effect<Message> messageTwo(Integer msg) {
      return effects().ignore();
    }
  }

  @Subscribe.Stream(id = "source", service = "abc")
  public static class StreamSubscriptionWithPublishToTopicAction extends Action {

    @Publish.Topic("foobar")
    public Effect<Message> messageOne(String msg) {
      return effects().reply(new Message(msg));
    }

    @Publish.Topic("foobar")
    public Effect<Message> messageTwo(Integer msg) {
      return effects().ignore();
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

    public UpdateEffect<Employee> onCreate(EmployeeCreated evt) {
      return effects()
        .updateState(new Employee(evt.firstName, evt.lastName, evt.email));
    }

    public UpdateEffect<Employee> onEmailUpdate(EmployeeEmailUpdated eeu) {
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

    public Effect<String> transform(EmployeeCreated created) {
      return effects().reply(created.toString());
    }

    public Effect<String> transform(EmployeeEmailUpdated emailUpdated) {
      return effects().reply(emailUpdated.toString());
    }

  }

  @Subscribe.Stream(service = "employee_service", id = "employee_events", ignoreUnknown = true)
  public static class EventStreamSubscriptionAction extends Action {

    public Effect<String> transform(EmployeeCreated created) {
      return effects().reply(created.toString());
    }

    public Effect<String> transform(EmployeeEmailUpdated emailUpdated) {
      return effects().reply(emailUpdated.toString());
    }
  }

  @Table(value = "employee_table")
  @Subscribe.Stream(service = "employee_service", id = "employee_events")
  public static class EventStreamSubscriptionView extends View<Employee> {

    public UpdateEffect<Employee> onCreate(EmployeeCreated evt) {
      return effects()
        .updateState(new Employee(evt.firstName, evt.lastName, evt.email));
    }

    public UpdateEffect<Employee> onEmailUpdate(EmployeeEmailUpdated eeu) {
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
