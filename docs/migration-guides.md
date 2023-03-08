# Migration Guides

## Migrating from 1.1.x to 1.2.x

### Java SDK

> **From 1.2.x onwards, the Java SDK is now called Java Protobuf SDK.**

Accordingly, the package dependencies were updated accordingly:
- from `kalix-java-sdk`to `kalix-java-sdk-protobuf`
- from `kalix-java-sdk-testkit` to `kalix-java-sdk-protobuf-testkit`.

Thus, in your `pom.xml`, instead of:

```xml
    <dependency>
      <groupId>io.kalix</groupId>
      <artifactId>kalix-java-sdk</artifactId>
      <version>${kalix-sdk.version}</version>
    </dependency>
    <dependency>
      <groupId>io.kalix</groupId>
      <artifactId>kalix-java-sdk-testkit</artifactId>
      <version>${kalix-sdk.version}</version>
      <scope>test</scope>
    </dependency>
```

Use:
```xml
    <dependency>
      <groupId>io.kalix</groupId>
      <artifactId>kalix-java-sdk-protobuf</artifactId>
      <version>${kalix-sdk.version}</version>
    </dependency>
    <dependency>
      <groupId>io.kalix</groupId>
      <artifactId>kalix-java-sdk-protobuf-testkit</artifactId>
      <version>${kalix-sdk.version}</version>
      <scope>test</scope>
    </dependency>
```

That's it!

### Scala SDK

>**From 1.2.x onwards, the Scala SDK is now called Scala Protobuf SDK.**

No changes required. The upgrade is a simple version bump here.

### Spring SDK

> **From 1.2.x onwards, the Spring SDK is now called Java SDK and is officially out of its beta version.**

The package dependencies were updated:
- from `kalix-spring-sdk`to `kalix-spring-boot-starter`
- from `kalix-spring-sdk-testkit` to `kalix-spring-boot-starter-test`

In the `pom.xml`, where previously there was:
```xml
    <dependency>
      <groupId>io.kalix</groupId>
      <artifactId>kalix-spring-sdk</artifactId>
      <version>${kalix-sdk.version}</version>
    </dependency>
    <dependency>
      <groupId>io.kalix</groupId>
      <artifactId>kalix-spring-sdk-testkit</artifactId>
      <version>${kalix-sdk.version}</version>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <version>3.0.2</version>
      <scope>test</scope>
    </dependency>
```

There should now be:
```xml
    <dependency>
      <groupId>io.kalix</groupId>
      <artifactId>kalix-spring-boot-starter</artifactId>
      <version>${kalix-sdk.version}</version>
    </dependency>
    <dependency>
      <groupId>io.kalix</groupId>
      <artifactId>kalix-spring-boot-starter-test</artifactId>
      <version>${kalix-sdk.version}</version>
      <scope>test</scope>
    </dependency>
```

#### Spring Boot Configurations

The following configurations are no longer needed and should be removed: 
- `spring.main.web-application-type=none` in `application.properties` (feel free to remove the file if that's the only property there)
- in your `Main.java`, you can now remove `@Import(KalixConfiguration.class)`

#### EventSourcedEntity with typed events

An `EventSourcedEntity<S>` is now `EventSourcedEntity<S, E>` where `E` is the top type for all the different event types such event-sourced entity will be allowed to emit. Although on the protocol-first SDKs mentioned in the previous sections this is taken care of automatically, for the code-first approach you need to provide that type. 

Thus, instead of :
```java
public class ShoppingCartEntity extends EventSourcedEntity<ShoppingCart> {
  // command and event handlers ...
}
```
There should be now:
```java
public class ShoppingCartEntity extends EventSourcedEntity<ShoppingCart, ShoppingCartEvent> {
  // command and event handlers ...
}
```

Where an example of this `ShoppingCartEvent`, could be:
```java
public sealed interface ShoppingCartEvent {

  @TypeName("item-added")
  record ItemAdded(ShoppingCart.LineItem item) implements ShoppingCartEvent { }

  @TypeName("item-removed")
  record ItemRemoved(String productId) implements ShoppingCartEvent { }
}
```

#### Kalix Annotations

All Kalix annotations moved from `kalix.springsdk.annotations.*` to `kalix.javasdk.annotations.*`.

Thus, when importing them, where before there was for instance:
```java
import kalix.springsdk.annotations.EntityKey;
import kalix.springsdk.annotations.EntityType;
import kalix.springsdk.annotations.EventHandler;
import kalix.springsdk.annotations.GenerateEntityKey;
```

It should now be:
```java
import kalix.javasdk.annotations.EntityKey;
import kalix.javasdk.annotations.EntityType;
import kalix.javasdk.annotations.EventHandler;
import kalix.javasdk.annotations.GenerateEntityKey;
```

Note these are only a few example of the existing annotations but all of them were moved.

#### TestKits

The Entity-specific test kits moved from `kalix.springsdk.testkit.*` to `kalix.javasdk.testkit.*`.

Thus, you will need to replace these:
```java
import kalix.springsdk.testkit.ActionTestkit;
import kalix.springsdk.testkit.EventSourcedTestKit;
import kalix.springsdk.testkit.ValueEntityTestKit;
```

With:

```java
import kalix.javasdk.testkit.ActionTestkit;
import kalix.javasdk.testkit.EventSourcedTestKit;
import kalix.javasdk.testkit.ValueEntityTestKit;
```

Lastly, `KalixIntegrationTestKitSupport` was also moved, so you will need to replace:
```java
import kalix.springsdk.testkit.KalixIntegrationTestKitSupport;
```
With:
```java
import kalix.spring.testkit.KalixIntegrationTestKitSupport;
```
