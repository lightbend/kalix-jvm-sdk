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

package com.lightbend.akkasls.codegen
package java

class EntityServiceSourceGeneratorSuite extends munit.FunSuite {

  test("EventSourcedEntity source") {

    val entity = TestData.eventSourcedEntity()
    val service = TestData.simpleEntityService()

    val packageName = "com.example.service"
    val className = "MyServiceEntity"
    val interfaceClassName = "AbstractMyServiceEntity"
    val entityType = "my-eventsourcedentity-persistence"

    val generatedSrc =
      EntityServiceSourceGenerator.source(
        service,
        entity,
        packageName,
        className,
        interfaceClassName,
        entityType
      )
    assertEquals(
      generatedSrc,
      """/* This code was initialised by Akka Serverless tooling.
      | * As long as this file exists it will not be re-generated.
      | * You are free to make changes to this file.
      | */
      |
      |package com.example.service;
      |
      |import com.akkaserverless.javasdk.EntityId;
      |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
      |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase;
      |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase.Effect;
      |import com.example.service.persistence.EntityOuterClass;
      |import com.external.Empty;
      |
      |/** An event sourced entity. */
      |@EventSourcedEntity(entityType = "my-eventsourcedentity-persistence")
      |public class MyServiceEntity extends AbstractMyServiceEntity {
      |    @SuppressWarnings("unused")
      |    private final String entityId;
      |    
      |    public MyServiceEntity(@EntityId String entityId) {
      |        this.entityId = entityId;
      |    }
      |    
      |    @Override
      |    public EntityOuterClass.MyState emptyState() {
      |        throw new UnsupportedOperationException("Not implemented yet, replace with your empty entity state");
      |    }
      |    
      |    @Override
      |    public Effect<Empty> set(EntityOuterClass.MyState currentState, ServiceOuterClass.SetValue setValue) {
      |        return effects().error("The command handler for `Set` is not implemented, yet");
      |    }
      |    
      |    @Override
      |    public Effect<ServiceOuterClass.MyState> get(EntityOuterClass.MyState currentState, ServiceOuterClass.GetValue getValue) {
      |        return effects().error("The command handler for `Get` is not implemented, yet");
      |    }
      |    
      |    @Override
      |    public EntityOuterClass.MyState setEvent(EntityOuterClass.MyState currentState, EntityOuterClass.SetEvent setEvent) {
      |        throw new RuntimeException("The event handler for `SetEvent` is not implemented, yet");
      |    }
      |}""".stripMargin
    )
  }

  test("Abstract EventSourcedEntity baseclass source") {
    val service = TestData.simpleEntityService()
    val entity = TestData.eventSourcedEntity()
    val packageName = "com.example.service"
    val className = "MyServiceEntity"

    val generatedSrc =
      EntityServiceSourceGenerator.interfaceSource(service, entity, packageName, className)
    assertEquals(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
      | * It will be re-generated to reflect any changes to your protobuf definitions.
      | * DO NOT EDIT
      | */
      |
      |package com.example.service;
      |
      |import com.akkaserverless.javasdk.eventsourcedentity.CommandHandler;
      |import com.akkaserverless.javasdk.eventsourcedentity.EventHandler;
      |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityBase;
      |import com.example.service.persistence.EntityOuterClass;
      |import com.external.Empty;
      |
      |/** An event sourced entity. */
      |public abstract class AbstractMyServiceEntity extends EventSourcedEntityBase<EntityOuterClass.MyState> {
      |    
      |    @CommandHandler
      |    public abstract Effect<Empty> set(EntityOuterClass.MyState currentState, ServiceOuterClass.SetValue setValue);
      |    
      |    @CommandHandler
      |    public abstract Effect<ServiceOuterClass.MyState> get(EntityOuterClass.MyState currentState, ServiceOuterClass.GetValue getValue);
      |    
      |    @EventHandler
      |    public abstract EntityOuterClass.MyState setEvent(EntityOuterClass.MyState currentState, EntityOuterClass.SetEvent setEvent);
      |}""".stripMargin
    )
  }

  test("EventSourcedEntity test source") {
    val service = TestData.simpleEntityService()
    val entity = TestData.eventSourcedEntity()

    val packageName = "com.example.service"
    val implClassName = "MyServiceEntity"
    val testClassName = "MyServiceEntityTest"

    val generatedSrc =
      EntityServiceSourceGenerator
        .testSource(
          service,
          entity,
          packageName,
          implClassName,
          testClassName
        )

    assertEquals(
      generatedSrc,
      """/* This code was initialised by Akka Serverless tooling.
        | * As long as this file exists it will not be re-generated.
        | * You are free to make changes to this file.
        | */
        |
        |package com.example.service;
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
        |    private MyServiceEntity entity;
        |    private CommandContext context = Mockito.mock(CommandContext.class);
        |    
        |    @Test
        |    public void setTest() {
        |        entity = new MyServiceEntity(entityId);
        |        
        |        // TODO: write your mock here
        |        // Mockito.when(context.[...]).thenReturn([...]);
        |        
        |        // TODO: set fields in command, and update assertions to verify implementation
        |        // assertEquals([expected],
        |        //    entity.set(ServiceOuterClass.SetValue.newBuilder().build(), context);
        |        // );
        |        
        |        // TODO: if you wish to verify events:
        |        //    Mockito.verify(context).emit(event);
        |    }
        |    
        |    @Test
        |    public void getTest() {
        |        entity = new MyServiceEntity(entityId);
        |        
        |        // TODO: write your mock here
        |        // Mockito.when(context.[...]).thenReturn([...]);
        |        
        |        // TODO: set fields in command, and update assertions to verify implementation
        |        // assertEquals([expected],
        |        //    entity.get(ServiceOuterClass.GetValue.newBuilder().build(), context);
        |        // );
        |        
        |        // TODO: if you wish to verify events:
        |        //    Mockito.verify(context).emit(event);
        |    }
        |}""".stripMargin
    )
  }

  test("Integration test source") {
    val mainPackageName = "com.example.service"
    val mainClassName = "SomeMain"

    val service = TestData.simpleEntityService()
    val entity = TestData.eventSourcedEntity()

    val packageName = "com.example.service"
    val integrationTestClassName = "MyServiceEntityIntegrationTest"

    val generatedSrc =
      EntityServiceSourceGenerator.integrationTestSource(
        mainPackageName,
        mainClassName,
        service,
        entity,
        packageName,
        integrationTestClassName
      )
    assertEquals(
      generatedSrc,
      """/* This code was initialised by Akka Serverless tooling.
        | * As long as this file exists it will not be re-generated.
        | * You are free to make changes to this file.
        | */
        |
        |package com.example.service;
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
