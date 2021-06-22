/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package java

class EntityServiceSourceGeneratorSuite extends munit.FunSuite {

  test("EventSourcedEntity source") {

    val entity  = TestData.eventSourcedEntity()
    val service = TestData.simpleEntityService()

    val packageName        = "com.example.service"
    val className          = "MyServiceEntityImpl"
    val interfaceClassName = "MyServiceEntityInterface"
    val entityType         = "my-eventsourcedentity-persistence"

    val sourceDoc =
      EntityServiceSourceGenerator.source(
        service,
        entity,
        packageName,
        className,
        interfaceClassName,
        entityType
      )
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
      |
      |import com.akkaserverless.javasdk.EntityId;
      |import com.akkaserverless.javasdk.eventsourcedentity.*;
      |import com.example.service.persistence.EntityOuterClass;
      |import com.external.Empty;
      |
      |/** An event sourced entity. */
      |@EventSourcedEntity(entityType = "my-eventsourcedentity-persistence")
      |public class MyServiceEntityImpl extends MyServiceEntityInterface {
      |    @SuppressWarnings("unused")
      |    private final String entityId;
      |    
      |    public MyServiceEntityImpl(@EntityId String entityId) {
      |        this.entityId = entityId;
      |    }
      |    
      |    @Override
      |    public EntityOuterClass.MyState snapshot() {
      |        // TODO: produce state snapshot here
      |        return EntityOuterClass.MyState.newBuilder().build();
      |    }
      |    
      |    @Override
      |    public void handleSnapshot(EntityOuterClass.MyState snapshot) {
      |        // TODO: restore state from snapshot here
      |        
      |    }
      |    
      |    @Override
      |    protected Empty set(ServiceOuterClass.SetValue command, CommandContext ctx) {
      |        throw ctx.fail("The command handler for `Set` is not implemented, yet");
      |    }
      |    
      |    @Override
      |    protected ServiceOuterClass.MyState get(ServiceOuterClass.GetValue command, CommandContext ctx) {
      |        throw ctx.fail("The command handler for `Get` is not implemented, yet");
      |    }
      |    
      |    @Override
      |    public void setEvent(EntityOuterClass.SetEvent event) {
      |        throw new RuntimeException("The event handler for `SetEvent` is not implemented, yet");
      |    }
      |}""".stripMargin
    )
  }

  test("ValueEntity source") {

    val service = TestData.simpleEntityService()
    val entity  = TestData.valueEntity()

    val packageName        = "com.example.service"
    val className          = "MyServiceImpl"
    val interfaceClassName = "MyServiceInterface"
    val entityType         = "my-valueentity-persistence"

    val sourceDoc =
      EntityServiceSourceGenerator.source(
        service,
        entity,
        packageName,
        className,
        interfaceClassName,
        entityType
      )
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
      |
      |import com.akkaserverless.javasdk.EntityId;
      |import com.akkaserverless.javasdk.valueentity.*;
      |import com.example.service.persistence.EntityOuterClass;
      |import com.external.Empty;
      |
      |/** A value entity. */
      |@ValueEntity(entityType = "my-valueentity-persistence")
      |public class MyServiceImpl extends MyServiceInterface {
      |    @SuppressWarnings("unused")
      |    private final String entityId;
      |    
      |    public MyServiceImpl(@EntityId String entityId) {
      |        this.entityId = entityId;
      |    }
      |    
      |    @Override
      |    protected Empty set(ServiceOuterClass.SetValue command, CommandContext<EntityOuterClass.MyState> ctx) {
      |        throw ctx.fail("The command handler for `Set` is not implemented, yet");
      |    }
      |    
      |    @Override
      |    protected ServiceOuterClass.MyState get(ServiceOuterClass.GetValue command, CommandContext<EntityOuterClass.MyState> ctx) {
      |        throw ctx.fail("The command handler for `Get` is not implemented, yet");
      |    }
      |}""".stripMargin
    )
  }

  test("EventSourcedEntity interface source") {
    val service     = TestData.simpleEntityService()
    val entity      = TestData.eventSourcedEntity()
    val packageName = "com.example.service"
    val className   = "MyServiceEntity"

    val sourceDoc =
      EntityServiceSourceGenerator.interfaceSource(service, entity, packageName, className)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
      |
      |import com.akkaserverless.javasdk.EntityId;
      |import com.akkaserverless.javasdk.Reply;
      |import com.akkaserverless.javasdk.eventsourcedentity.*;
      |import com.example.service.persistence.EntityOuterClass;
      |import com.external.Empty;
      |
      |/** An event sourced entity. */
      |public abstract class MyServiceEntityInterface {
      |    
      |    public class CommandNotImplementedException extends UnsupportedOperationException {
      |        public CommandNotImplementedException() {
      |            super("You have either created a new command or removed the handling of an existing command. Please declare a method in your \"impl\" class for this command.");
      |        }
      |    }
      |    
      |    @Snapshot
      |    public abstract EntityOuterClass.MyState snapshot();
      |    
      |    @SnapshotHandler
      |    public abstract void handleSnapshot(EntityOuterClass.MyState snapshot);
      |    
      |    @CommandHandler(name = "Set")
      |    public Reply<Empty> setWithReply(ServiceOuterClass.SetValue command, CommandContext ctx) {
      |        return Reply.message(set(command, ctx));
      |    }
      |    
      |    protected Empty set(ServiceOuterClass.SetValue command, CommandContext ctx) {
      |        return set(command);
      |    }
      |    
      |    protected Empty set(ServiceOuterClass.SetValue command) {
      |        throw new CommandNotImplementedException();
      |    }
      |    
      |    @CommandHandler(name = "Get")
      |    public Reply<ServiceOuterClass.MyState> getWithReply(ServiceOuterClass.GetValue command, CommandContext ctx) {
      |        return Reply.message(get(command, ctx));
      |    }
      |    
      |    protected ServiceOuterClass.MyState get(ServiceOuterClass.GetValue command, CommandContext ctx) {
      |        return get(command);
      |    }
      |    
      |    protected ServiceOuterClass.MyState get(ServiceOuterClass.GetValue command) {
      |        throw new CommandNotImplementedException();
      |    }
      |    
      |    @EventHandler
      |    public abstract void setEvent(EntityOuterClass.SetEvent event);
      |}""".stripMargin
    )
  }

  test("ValueEntity interface source") {
    val service     = TestData.simpleEntityService()
    val entity      = TestData.valueEntity()
    val packageName = "com.example.service"
    val className   = "MyService"

    val sourceDoc =
      EntityServiceSourceGenerator.interfaceSource(service, entity, packageName, className)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
      |
      |import com.akkaserverless.javasdk.EntityId;
      |import com.akkaserverless.javasdk.Reply;
      |import com.akkaserverless.javasdk.valueentity.*;
      |import com.example.service.persistence.EntityOuterClass;
      |import com.external.Empty;
      |
      |/** A value entity. */
      |public abstract class MyServiceInterface {
      |    
      |    public class CommandNotImplementedException extends UnsupportedOperationException {
      |        public CommandNotImplementedException() {
      |            super("You have either created a new command or removed the handling of an existing command. Please declare a method in your \"impl\" class for this command.");
      |        }
      |    }
      |    
      |    @CommandHandler(name = "Set")
      |    public Reply<Empty> setWithReply(ServiceOuterClass.SetValue command, CommandContext<EntityOuterClass.MyState> ctx) {
      |        return Reply.message(set(command, ctx));
      |    }
      |    
      |    protected Empty set(ServiceOuterClass.SetValue command, CommandContext<EntityOuterClass.MyState> ctx) {
      |        return set(command);
      |    }
      |    
      |    protected Empty set(ServiceOuterClass.SetValue command) {
      |        throw new CommandNotImplementedException();
      |    }
      |    
      |    @CommandHandler(name = "Get")
      |    public Reply<ServiceOuterClass.MyState> getWithReply(ServiceOuterClass.GetValue command, CommandContext<EntityOuterClass.MyState> ctx) {
      |        return Reply.message(get(command, ctx));
      |    }
      |    
      |    protected ServiceOuterClass.MyState get(ServiceOuterClass.GetValue command, CommandContext<EntityOuterClass.MyState> ctx) {
      |        return get(command);
      |    }
      |    
      |    protected ServiceOuterClass.MyState get(ServiceOuterClass.GetValue command) {
      |        throw new CommandNotImplementedException();
      |    }
      |}""".stripMargin
    )
  }

  test("EventSourcedEntity test source") {
    val service = TestData.simpleEntityService()
    val entity  = TestData.eventSourcedEntity()

    val packageName   = "com.example.service"
    val implClassName = "MyServiceEntityImpl"
    val testClassName = "MyServiceEntityTest"

    val sourceDoc =
      EntityServiceSourceGenerator.testSource(
        service,
        entity,
        packageName,
        implClassName,
        testClassName
      )
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
        |import com.external.Empty;
        |import org.junit.Test;
        |import org.mockito.*;
        |
        |import static org.junit.Assert.assertThrows;
        |
        |public class MyServiceEntityTest {
        |    private String entityId = "entityId1";
        |    private MyServiceEntityImpl entity;
        |    private CommandContext context = Mockito.mock(CommandContext.class);
        |    
        |    private class MockedContextFailure extends RuntimeException {};
        |    
        |    @Test
        |    public void setTest() {
        |        entity = new MyServiceEntityImpl(entityId);
        |        
        |        Mockito.when(context.fail("The command handler for `Set` is not implemented, yet"))
        |            .thenReturn(new MockedContextFailure());
        |        
        |        // TODO: set fields in command, and update assertions to match implementation
        |        assertThrows(MockedContextFailure.class, () -> {
        |            entity.setWithReply(ServiceOuterClass.SetValue.newBuilder().build(), context);
        |        });
        |        
        |        // TODO: if you wish to verify events:
        |        //    Mockito.verify(context).emit(event);
        |    }
        |    
        |    @Test
        |    public void getTest() {
        |        entity = new MyServiceEntityImpl(entityId);
        |        
        |        Mockito.when(context.fail("The command handler for `Get` is not implemented, yet"))
        |            .thenReturn(new MockedContextFailure());
        |        
        |        // TODO: set fields in command, and update assertions to match implementation
        |        assertThrows(MockedContextFailure.class, () -> {
        |            entity.getWithReply(ServiceOuterClass.GetValue.newBuilder().build(), context);
        |        });
        |        
        |        // TODO: if you wish to verify events:
        |        //    Mockito.verify(context).emit(event);
        |    }
        |}""".stripMargin
    )
  }

  test("ValueEntity test source") {
    val service = TestData.simpleEntityService()
    val entity  = TestData.valueEntity()

    val packageName   = "com.example.service"
    val implClassName = "MyServiceImpl"
    val testClassName = "MyServiceTest"

    val sourceDoc =
      EntityServiceSourceGenerator.testSource(
        service,
        entity,
        packageName,
        implClassName,
        testClassName
      )
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.valueentity.CommandContext;
        |import com.example.service.persistence.EntityOuterClass;
        |import com.external.Empty;
        |import org.junit.Test;
        |import org.mockito.*;
        |
        |import static org.junit.Assert.assertThrows;
        |
        |public class MyServiceTest {
        |    private String entityId = "entityId1";
        |    private MyServiceImpl entity;
        |    private CommandContext<EntityOuterClass.MyState> context = Mockito.mock(CommandContext.class);
        |    
        |    private class MockedContextFailure extends RuntimeException {};
        |    
        |    @Test
        |    public void setTest() {
        |        entity = new MyServiceImpl(entityId);
        |        
        |        Mockito.when(context.fail("The command handler for `Set` is not implemented, yet"))
        |            .thenReturn(new MockedContextFailure());
        |        
        |        // TODO: set fields in command, and update assertions to match implementation
        |        assertThrows(MockedContextFailure.class, () -> {
        |            entity.setWithReply(ServiceOuterClass.SetValue.newBuilder().build(), context);
        |        });
        |    }
        |    
        |    @Test
        |    public void getTest() {
        |        entity = new MyServiceImpl(entityId);
        |        
        |        Mockito.when(context.fail("The command handler for `Get` is not implemented, yet"))
        |            .thenReturn(new MockedContextFailure());
        |        
        |        // TODO: set fields in command, and update assertions to match implementation
        |        assertThrows(MockedContextFailure.class, () -> {
        |            entity.getWithReply(ServiceOuterClass.GetValue.newBuilder().build(), context);
        |        });
        |    }
        |}""".stripMargin
    )
  }

  test("Integration test source") {
    val mainPackageName = "com.example.service"
    val mainClassName   = "SomeMain"

    val service = TestData.simpleEntityService()
    val entity  = TestData.eventSourcedEntity()

    val packageName              = "com.example.service"
    val integrationTestClassName = "MyServiceEntityIntegrationTest"

    val sourceDoc =
      EntityServiceSourceGenerator.integrationTestSource(
        mainPackageName,
        mainClassName,
        service,
        entity,
        packageName,
        integrationTestClassName
      )
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.example.service.SomeMain;
        |import com.example.service.MyServiceClient;
        |import com.akkaserverless.javasdk.testkit.junit.AkkaServerlessTestkitResource;
        |import org.junit.ClassRule;
        |import org.junit.Test;
        |
        |import static java.util.concurrent.TimeUnit.*;
        |
        |// Example of an integration test calling our service via the Akka Serverless proxy
        |// Run all test classes ending with "IntegrationTest" using `mvn verify -Pit`
        |public class MyServiceEntityIntegrationTest {
        |    
        |    /**
        |     * The test kit starts both the service container and the Akka Serverless proxy.
        |     */
        |    @ClassRule
        |    public static final AkkaServerlessTestkitResource testkit = new AkkaServerlessTestkitResource(SomeMain.SERVICE);
        |    
        |    /**
        |     * Use the generated gRPC client to call the service through the Akka Serverless proxy.
        |     */
        |    private final MyServiceClient client;
        |    
        |    public MyServiceEntityIntegrationTest() {
        |        client = MyServiceClient.create(testkit.getGrpcClientSettings(), testkit.getActorSystem());
        |    }
        |    
        |    @Test
        |    public void setOnNonExistingEntity() throws Exception {
        |        // TODO: set fields in command, and provide assertions to match replies
        |        // client.set(ServiceOuterClass.SetValue.newBuilder().build())
        |        //         .toCompletableFuture().get(2, SECONDS);
        |    }
        |    
        |    @Test
        |    public void getOnNonExistingEntity() throws Exception {
        |        // TODO: set fields in command, and provide assertions to match replies
        |        // client.get(ServiceOuterClass.GetValue.newBuilder().build())
        |        //         .toCompletableFuture().get(2, SECONDS);
        |    }
        |}""".stripMargin
    )
  }

}
