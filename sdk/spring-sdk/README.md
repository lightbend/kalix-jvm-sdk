# Kalix Spring SDK

(Note: since the Spring SDK is very recent, there is not yet proper documentation so this document aims to serve as a general introduction guide.)

The goal of the Spring SDK is to provide a code-first approach to developing Kalix applications so that a developer does not need to worry about learning protobuf upfront.

This SDK offers a way to develop applications which should be familiar to someone who has worked with Spring applications by making use of annotations to assemble Kalix and its components.

> If you're new to Kalix and the different types of entities that exist in a Kalix app, consider reading [this section](https://docs.kalix.io/services/programming-model.html#_what_is_a_kalix_service) before continuing.  

## Getting Started

The easiest way to get started is to use the `kalix-spring-boot-archetype` and let it generate a Spring Boot application for you. Try:

```shell
mvn \
archetype:generate \
-DarchetypeGroupId=io.kalix \
-DarchetypeArtifactId=kalix-spring-boot-archetype \
-DarchetypeVersion=LATEST
```

Inside the generated project you can find a `README.md` file with further instructions on how to run the project. On the next sections, we will go through the main features that are available and how to use them.

## Features

As the Spring SDK is more recent than their gRPC-first counterparts, not all features are supported at this time. However, there's already a lot to play with:

- Actions
- Value Entities
- EventSourced Entities
- Views
- Publishing and subscribing to Topics

> **Note:** the code snippets shared in the following sections are illustrative and might not be complete. For working samples, make sure to check the `Sample` sub-section at the end of each section. 

### Actions

An Action can be defined by:

1. creating a class extending the `Action` interface
2. using Spring's RequestMapping annotations to define the routes to your entity and implement the command handlers

*EchoAction.java*

```java
// ...
import kalix.javasdk.action.Action;

public class EchoAction extends Action { // <1>

  @GetMapping("/echo/{msg}") // <2>
  public Effect<Message> stringMessage(@PathVariable String msg) {
    return effects().reply(new Message(msg));
  }

}
```

#### Sample

If you're looking for a working sample of an action, check [samples/spring-fibonacci-action](../../samples/spring-fibonacci-action).

### Value Entities

To define a Value Entity, one will need to do 4 simple steps:

1. create a class that extends `ValueEntity<S>`, where `S` is the state type this entity will store (what might usually be called a domain model class)
2. make sure to annotate such class with `@EntityKey` and pass name of the path parameter that will be used as the
   entity unique identifier
3. make sure to annotate such class with `@EntityType` and pass a unique name for this entity type  
4. make use of Spring's RequestMapping annotation to define the routes to your entity
5. declare the methods that will handle the requests and update the state of the entity

*UserEntity.java*

```java
// ...

import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.javasdk.valueentity.ValueEntity;

@EntityKey("id") // <2>
@EntityType("user") // <3>
@RequestMapping("/user/{id}") // <4>
public class UserEntity extends ValueEntity<User> { // <1>

   @PostMapping("/{email}/{name}") // <4>
   public Effect<String> createOrUpdate(@PathVariable String email, @PathVariable String name) {
      return effects()
              .updateState(new User(email, name)) // <5>
              .thenReply("Ok");
   }
}
```
From the above example, note:
- the EntityKey `id` needs to match a path parameter and such value needs to be unique per entity
- the EntityType `user` is common for all instances of this entity but should be unique across the different entity 
  types
- the API to update the state is the same as the Java one (and is provided by `effects()`)


Below you can find a simple example of what the `User` model for the above entity could look like:

*User.java*

```java
public class User {
  public String email;
  public String name;
  
  @JsonCreator
  public User(String email, String name) {
    this.email = email;
    this.name = name;
  }
}
```

#### Sample

If you're looking for a working sample of a value entity, check [samples/spring-customer-registry-views-quickstart](../../samples/spring-customer-registry-views-quickstart). 


### Event-sourced Entity

To define an Event-Sourced Entity, be aware of the following aspects:
1. create a class that extends `EventSourcedEntity<S>`, where `S` is the state type this entity will store (what might usually be called a domain model class)
2. make sure to annotate such class with `@EntityKey` and pass name of the path parameter that will be used as the
   entity unique identifier
3. make sure to annotate such class with `@EntityType` and pass a unique name for this entity type  
4. make use of Spring's RequestMapping annotation to define the routes to your entity
5. declare the methods that will handle the requests/commands and emit the outgoing events
6. define the methods that will serve as handlers of the events generated by the commands by marking such methods with 
   `@EventHandler`

*CounterEntity.java*

```java
// ...

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;

@EntityKey("id") // <2>
@EntityType("counter") // <3>
@RequestMapping("/counter/{id}") // <4>
public class CounterEntity extends EventSourcedEntity<Counter> { // <1>

   @Override
   public Counter emptyState() {
      return new Counter(0);
   }

   @PostMapping("/increase/{value}") // <4>
   public Effect<Integer> increase(@PathVariable Integer value) { // <5>
      return effects()
              .emitEvent(new ValueIncreased(value))
              .thenReply(c -> c.value);
   }

   @GetMapping // <4>
   public Effect<String> get() {
      return effects().reply(currentState().value.toString());
   }

   @EventHandler // <6>
   public Counter handleIncrease(ValueIncreased increased) {
      return currentState().onValueIncreased(increased);
   }

}
```

From the above example, note that:

- `EntityKey` and `EntityType` have the same semantics as previously explained for Value Entities
- the `emptyState()` override is optional. If not using it, be careful to deal with a currentState() with a `null` value when receiving the first command
- there needs to be one event handler declared per each type of event the ES entity emits (e.g `handleIncrease` receives a param of type `ValueIncreased`, the same type emitted in `increase` command handler)

Below you can find a simple example of what the Counter model for the above entity could look like:

*Counter.java*

```java
public class Counter {
  public final Integer value;

  @JsonCreator
  public Counter(@JsonProperty Integer value) {
    this.value = value;
  }

  public Counter increase(int byNum) {
    return new Counter(this.value + byNum);
  }

  public Counter onValueIncreased(ValueIncreased evt) {
    return new Counter(this.value + evt.value);
  }
}
```

#### Sample

If you're looking for a working sample of an event-sourced entity, check [samples/spring-eventsourced-counter](../../samples/spring-eventsourced-counter).

### Views

Generally, a view is defined in 3 steps:

1. creating a class that extends `View<S>`, where `S` is the event type this view will receive (what might usually be called a domain model class)
2. defining the source of events/updates by using one the `@Subscribe` annotations
3. providing a way to query the data using Spring's RequestMapping and Kalix `@Query` annotations 

There are the slight differences depending on the type of entity the view is subscribing to, which we will describe  next. 

#### From a Value Entity 

To subscribe to a Value Entity, use `@Subscribe.ValueEntity` annotation to provide the respective Entity type as follows:

*UsersByEmail.java*

```java
// ...
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;

@Table("users_by_email")
@Subscribe.ValueEntity(UserEntity.class)
public class UsersByEmail extends View<User> {

  @GetMapping("/users/by_email/{email}")
  @Query("SELECT * FROM users_by_email WHERE email = :email")
  public User getUsers(String email) {
    return null;
  }
}
```

From the above example, note that:

- a value identifying the view must be provided to `@Table`
- we are subscribing to updates of `UserEntity` (from the example provided in the [Value Entities section](#value-entities))
- the type of updates is `User` as defined in `extends View<User>` since that is the state type of the `UserEntity`
- the query method `getUsers` needs to have a dummy implementation for now (which is not used)

#### Transformed view state

In several contexts, it might be useful to transform the incoming updates to a different model. In such scenario:

1. the `@Subscribe` annotation will be at a method level (instead of class level)
2. the event handler method will be used to update the state to its new version
3. the query method needs to return the new state type

*UserWithVersionView.java*

```java
@Table("users_with_version")
public class UserWithVersionView extends View<UserWithVersion> {

  @Subscribe.ValueEntity(UserEntity.class) // <1>
  public UpdateEffect<UserWithVersion> onChange(User user) { // <2>
    if (viewState() == null) 
      return effects()
          .updateState(new UserWithVersion(user.email, 1));
    else 
      return effects()
          .updateState(new UserWithVersion(user.email, viewState().version + 1));
  }

  @Query("SELECT * FROM user_view WHERE email = :email")
  @GetMapping("/users/by-email/{email}")
  public UserWithVersion getUser(@PathVariable String email) { // <3>
    return null;
  }
}
```

From the above example, note that:

- the type of the state is now `View<UserWithVersion>` instead of `View<User>`
- it is still subscribing to a Value Entity but now the updated return type of both the event and query handlers is of type `UserWithVersion`   

#### Sample

If you're looking for a working sample using a view, check [samples/spring-customer-registry-views-quickstart](../../samples/spring-customer-registry-views-quickstart).

#### From an Event-sourced Entity

Following a similar pattern from the above, there is also a possibility of creating a view that processes the events from an Event-sourced Entity. In such scenario, make sure to:

1. create a class that extends `View<S>`, where `S` is the transformed state type this view will work on
2. define a handler method to process the events and update view state accordingly by using  `@Subscribe.EventSourcedEntity` annotation
3. provide a query to access the data as intended

*UserByEmailView.java*

```java
// ...
import kalix.javasdk.view.View;
import kalix.springsdk.annotations.Query;
import kalix.springsdk.annotations.Subscribe;
import kalix.springsdk.annotations.Table;

@Table("users_by_email")
public class UserByEmailView extends View<UserView> { // <1>

  @GetMapping("/user/by_email/{email}")
  @Query("SELECT * FROM users_by_email WHERE email = :email") // <3>
  public UserView getUser(String email) {
    return null;
  }

  @Subscribe.EventSourcedEntity(UserEntity.class) // <2>
  public UpdateEffect<UserView> onEvent(UserView user, UserEvent event) {
    return UserView.onEvent(user, event)
        .map(state -> effects().updateState(state))
        .orElse(effects().ignore());
  }
}
```

If you looked close enough, some questions might be popping up, such as:

1. What events are being produced by the `UserEntity.class` we are subscribing to?
   - for this example, we are considering it to emit two types of events `UserCreated` and `NameChanged`. As you can see below, both types derive from the `UserEvent` used. Their implementations are trivial (and thus not shown here). 

*UserEvent.java*

```java
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
  {
    @JsonSubTypes.Type(value = UserCreated.class, name = "user-created"),
    @JsonSubTypes.Type(value = NameChanged.class, name = "name-changed"),
  })
public interface UserEvent { }
```

2. how does the `UserView.onEvent` work?
   - on the event handler we need to take advantage of the type hierarchy to identify the specific event we are dealing with to act accordingly
> Note: we are working on allowing you to declare a separate event handler per event type
   
*UserView.java*

```java
// ...
public class UserView {
  // ... constructor, setters and getters ...

  public static Optional<UserView> onEvent(UserView state, UserEvent event) {

    if (state == null) {
      // the only event we can receive when state is null is the UserCreated
      if (event instanceof UserCreated) {
        UserCreated created = (UserCreated) event;
        return Optional.of(new UserView(created.email, created.name, created.address));
      }
    } else {
      // when not null, we can receive the other events
      if (event instanceof NameChanged) {
        NameChanged nameChanged = (NameChanged) event;
        return Optional.of(state.withName(nameChanged.newName));
      }
    }

    return Optional.empty();
  }
}
```

Lastly, to complete the example here's how the entity generates the events that are consumed on the view presented above.


*UserEntity.java*

```java
// ...

import kalix.javasdk.eventsourcedentity.EventSourcedEntity;
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;

@EntityKey("id")
@EntityType("es_users")
@RequestMapping("/user/{id}")
public class UserEntity extends EventSourcedEntity<User> {

   @GetMapping
   public Effect<User> getUser() {
      return effects().reply(currentState());
   }

   @PostMapping("/create")
   public Effect<String> create(@RequestBody User user) {
      return effects()
              .emitEvent(new UserCreated(user.email, user.name))
              .thenReply(__ -> "OK");
   }

   @EventHandler
   public User onEvent(UserCreated created) {
      return new User(created.email, created.name);
   }

   @PostMapping("/changeName/{newName}")
   public Effect<String> changeName(@PathVariable String newName) {
      return effects()
              .emitEvent(new NameChanged(newName))
              .thenReply(__ -> "OK");
   }

   @EventHandler
   public User onEvent(NameChanged nameChanged) {
      return currentState().withName(nameChanged.newName);
   }
}
```

#### Sample

If you're looking for a working sample of a view subscribing to an Event-sourced Entity, check [samples/spring-eventsourced-customer-registry](../../samples/spring-eventsourced-customer-registry).

## Advanced Actions

The previous section about Actions was a brief introduction, but they offer  additional functionalities that we will visit below.

### Subscribing to topics

To use an Action to consume a general PubSub topic, make sure to:

1. create a class extending `Action`
2. annotate a method handler with `@Subscribe.Topic` and make sure the parameter matches the type of messages that will be consumed

*SubscribeToTwoTopicsAction.java*

```java
public class SubscribeToTwoTopicsAction extends Action { // <1>

    @Subscribe.Topic("topicXYZ") 
    public Action.Effect<Message> methodOne(Message message) { // <2>
      return effects().reply(message);
    }

    @Subscribe.Topic("topicXYZ")
    public Action.Effect<Message2> methodTwo(Message2 message) {
      return effects().reply(message);
    }
}
```

### Publishing to topics

To use an Action to publish to a PubSub topic, make sure to:

1. create a class extending `Action`
2. annotate a method handler with `@Publish.Topic`
   1. this can get combined with a `@RequestMapping` type annotation

*PublishToTopicAction.java*

```java
public class PublishToTopicAction extends Action { // <1>

   @PostMapping("/message/{msg}")
   @Publish.Topic("foobar")
   public Effect<Message> messageOne(@PathVariable String msg) { // <2>
      return effects().reply(new Message(msg));
   }
}
```

### Cross-components calls

At this moment, the ability to call external Kalix components is limited to two options:

1. use a regular HTTP client of your preference and call the service with its full external path;
2. use a provided `KalixClient` that allows one to do a simple local call to another component (see example below).

```java
import kalix.javasdk.action.Action;
import kalix.javasdk.action.ActionCreationContext;
import kalix.springsdk.KalixClient;

public class ShortenedEchoAction extends Action {

  private ActionCreationContext ctx;
  private KalixClient kalixClient;

  public ShortenedEchoAction(ActionCreationContext ctx, KalixClient kalixClient) {
    this.ctx = ctx;
    this.kalixClient = kalixClient;
  }

  @GetMapping("/echo/message/{msg}/short")
  public Effect<Message> stringMessage(@PathVariable String msg) {
    var shortenedMsg = msg.replaceAll("[AEIOUaeiou]", "");
    var result = kalixClient.get("/echo/message/" + shortenedMsg, Message.class).execute();
    return effects().asyncReply(result);
  }
}
```

From the example above, note:

- access to the `KalixClient` is provided by Kalix at runtime given that there is a constructor receiving a parameter with such type;
- the client resembles a normal HTTP client, providing `.get` and `.post` requests that can then be executed and passed as reply.


## Testing

For now, only Integration tests are available to test your application. You can do such a test by having your test classes extending `KalixIntegrationTestKitSupport`. Here is an example:

```java
// ...
import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Main.class)
public class FibonacciActionIntegrationTest extends KalixIntegrationTestKitSupport {
   @Autowired
   private WebClient webClient;

   @Test
   public void calculateNextNumber() {
      Mono<Number> response =
           webClient.get()
                .uri("/fibonacci/5/next")
                .retrieve().bodyToMono(Number.class);

      long next = response.block(Duration.of(5, SECONDS)).value;
      Assertions.assertEquals(8, next);
   }
}
```

> **Note:** all samples mentioned earlier include some Integration Tests as well that can be consulted if one is looking for real examples. 