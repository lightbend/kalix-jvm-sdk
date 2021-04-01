/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package java

import org.apache.commons.io.FileUtils

import _root_.java.nio.file.Files

class SourceGeneratorSuite extends munit.FunSuite {

  test("generate") {
    val sourceDirectory = Files.createTempDirectory("source-generator-test")
    try {

      val testSourceDirectory = Files.createTempDirectory("test-source-generator-test")

      try {

        val source1     = sourceDirectory.resolve("com/lightbend/MyService1.java")
        val sourceFile1 = source1.toFile
        FileUtils.forceMkdir(sourceFile1.getParentFile)
        FileUtils.touch(sourceFile1)

        val testSource2     = testSourceDirectory.resolve("com/lightbend/MyService2Test.java")
        val testSourceFile2 = testSource2.toFile
        FileUtils.forceMkdir(testSourceFile2.getParentFile)
        FileUtils.touch(testSourceFile2)

        val entities = List(
          ModelBuilder.EventSourcedEntity(
            Some("com/lightbend"),
            Some("MyEntity1"),
            "com.lightbend.MyService1",
            "MyService1",
            Some("com.lightbend.State1"),
            List(
              ModelBuilder.Command(
                "com.lightbend.MyService.Set",
                "com.lightbend.SetValue",
                "com.google.protobuf.Empty"
              ),
              ModelBuilder.Command(
                "com.lightbend.MyService.Get",
                "com.lightbend.GetValue",
                "com.lightbend.MyState"
              )
            ),
            List.empty
          ),
          ModelBuilder.EventSourcedEntity(
            Some("com/lightbend"),
            Some("MyEntity2"),
            "com.lightbend.MyService2",
            "MyService2",
            Some("com.lightbend.State2"),
            List(
              ModelBuilder.Command(
                "com.lightbend.MyService.Set",
                "com.lightbend.SetValue",
                "com.google.protobuf.Empty"
              ),
              ModelBuilder.Command(
                "com.lightbend.MyService.Get",
                "com.lightbend.GetValue",
                "com.lightbend.MyState"
              )
            ),
            List.empty
          ),
          ModelBuilder.EventSourcedEntity(
            Some("com/lightbend"),
            Some("MyEntity3"),
            "com.lightbend.something.MyService3",
            "MyService3",
            Some("com.lightbend.State3"),
            List(
              ModelBuilder.Command(
                "com.lightbend.MyService.Set",
                "com.lightbend.SetValue",
                "com.google.protobuf.Empty"
              ),
              ModelBuilder.Command(
                "com.lightbend.MyService.Get",
                "com.lightbend.GetValue",
                "com.lightbend.MyState"
              )
            ),
            List.empty
          )
        )

        val sources = SourceGenerator.generate(
          entities,
          sourceDirectory,
          testSourceDirectory,
          "com.lightbend.Main"
        )

        assertEquals(Files.size(source1), 0L)
        assertEquals(Files.size(testSource2), 0L)

        assertEquals(
          sources,
          List(
            sourceDirectory.resolve("com/lightbend/MyService2.java"),
            sourceDirectory.resolve("com/lightbend/something/MyService3.java"),
            testSourceDirectory.resolve("com/lightbend/something/MyService3Test.java"),
            sourceDirectory.resolve("com/lightbend/Main.java")
          )
        )

        // Test that the main, source and test files are being written to
        assertEquals(Files.readAllBytes(sources.head).head.toChar, 'p')
        assertEquals(Files.readAllBytes(sources.drop(1).head).head.toChar, 'p')
        assertEquals(Files.readAllBytes(sources.drop(3).head).head.toChar, 'p')

      } finally FileUtils.deleteDirectory(testSourceDirectory.toFile)
    } finally FileUtils.deleteDirectory(sourceDirectory.toFile)
  }

  test("source") {
    val entity = ModelBuilder.EventSourcedEntity(
      Some("com/lightbend"),
      Some("MyEntity"),
      "com.lightbend.MyServiceEntity",
      "MyServiceEntity",
      Some("com.lightbend.MyState"),
      List(
        ModelBuilder.Command(
          "com.lightbend.MyServiceEntity.Set",
          "com.lightbend.SetValue",
          "google.protobuf.Empty"
        ),
        ModelBuilder.Command(
          "com.lightbend.MyServiceEntity.Get",
          "com.lightbend.GetValue",
          "com.lightbend.MyState"
        )
      ),
      List(
        "com.lightbend.SetEvent"
      )
    )
    val packageName = "com.lightbend"
    val className   = "MyServiceEntity"

    val sourceDoc = SourceGenerator.source(entity, packageName, className)
    assertEquals(
      sourceDoc.layout,
      """package com.lightbend;
      |
      |import com.google.protobuf.Empty;
      |import com.akkaserverless.javasdk.EntityId;
      |import com.akkaserverless.javasdk.eventsourcedentity.*;
      |
      |/** An event sourced entity. */
      |@EventSourcedEntity(entityType = "MyServiceEntity")
      |public class MyServiceEntity {
      |    @SuppressWarnings("unused")
      |    private final String entityId;
      |    
      |    public MyServiceEntity(@EntityId String entityId) {
      |        this.entityId = entityId;
      |    }
      |    
      |    @Snapshot
      |    public MyEntity.MyState snapshot() {
      |        // TODO: produce state snapshot here
      |        return MyEntity.MyState.newBuilder().setEntityId(this.entityId).build();
      |    }
      |    
      |    @SnapshotHandler
      |    public void handleSnapshot(MyEntity.MyState snapshot) {
      |        // TODO: restore state from snapshot here
      |        this.entityId = snapshot.entityId;
      |    }
      |    
      |    @CommandHandler
      |    public Empty set(MyEntity.SetValue command, CommandContext ctx) {
      |        throw new RuntimeException("The command handler for `Set` is not implemented, yet");
      |    }
      |    
      |    @CommandHandler
      |    public MyEntity.MyState get(MyEntity.GetValue command, CommandContext ctx) {
      |        throw new RuntimeException("The command handler for `Get` is not implemented, yet");
      |    }
      |    
      |    @EventHandler
      |    public void setEvent(MyEntity.SetEvent event) {
      |        throw new RuntimeException("The event handler for `SetEvent` is not implemented, yet");
      |    }
      |}""".stripMargin
    )
  }

  test("test source") {
    val entity = ModelBuilder.EventSourcedEntity(
      Some("com/lightbend"),
      Some("MyEntity"),
      "com.lightbend.MyServiceEntity",
      "MyServiceEntity",
      Some("com.lightbend.MyState"),
      List(
        ModelBuilder.Command(
          "com.lightbend.MyServiceEntity.Set",
          "com.lightbend.SetValue",
          "google.protobuf.Empty"
        ),
        ModelBuilder.Command(
          "com.lightbend.MyServiceEntity.Get",
          "com.lightbend.GetValue",
          "com.lightbend.MyState"
        )
      ),
      List.empty
    )
    val packageName   = "com.lightbend"
    val className     = "MyServiceEntity"
    val testClassName = "MyServiceEntityTest"

    val sourceDoc = SourceGenerator.testSource(entity, packageName, className, testClassName)
    assertEquals(
      sourceDoc.layout,
      """package com.lightbend;
        |
        |import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
        |import org.junit.Test;
        |import org.mockito.*;
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
        |        // TODO: you may want to set fields in addition to the entity id
        |        //    entity.set(MyEntity.SetValue.newBuilder().setEntityId(entityId).build(), context);
        |        
        |        // TODO: if you wish to verify events:
        |        //    Mockito.verify(context).emit(event);
        |    }
        |    
        |    @Test
        |    public void getTest() {
        |        entity = new MyServiceEntity(entityId);
        |        
        |        // TODO: you may want to set fields in addition to the entity id
        |        //    entity.get(MyEntity.GetValue.newBuilder().setEntityId(entityId).build(), context);
        |        
        |        // TODO: if you wish to verify events:
        |        //    Mockito.verify(context).emit(event);
        |    }
        |}""".stripMargin
    )
  }

  test("main source") {
    val entities = List(
      ModelBuilder.EventSourcedEntity(
        Some("com/lightbend"),
        Some("MyEntity1"),
        "com.lightbend.MyService1",
        "MyService1",
        Some("com.lightbend.State1"),
        List(
          ModelBuilder.Command(
            "com.lightbend.MyService.Set",
            "com.lightbend.SetValue",
            "com.google.protobuf.Empty"
          ),
          ModelBuilder.Command(
            "com.lightbend.MyService.Get",
            "com.lightbend.GetValue",
            "com.lightbend.MyState"
          )
        ),
        List.empty
      ),
      ModelBuilder.EventSourcedEntity(
        Some("com/lightbend"),
        Some("MyEntity2"),
        "com.lightbend.MyService2",
        "MyService2",
        Some("com.lightbend.State2"),
        List(
          ModelBuilder.Command(
            "com.lightbend.MyService.Set",
            "com.lightbend.SetValue",
            "com.google.protobuf.Empty"
          ),
          ModelBuilder.Command(
            "com.lightbend.MyService.Get",
            "com.lightbend.GetValue",
            "com.lightbend.MyState"
          )
        ),
        List.empty
      ),
      ModelBuilder.EventSourcedEntity(
        Some("com/lightbend"),
        Some("MyEntity3"),
        "com.lightbend.something.MyService3",
        "MyService3",
        Some("com.lightbend.State3"),
        List(
          ModelBuilder.Command(
            "com.lightbend.MyService.Set",
            "com.lightbend.SetValue",
            "com.google.protobuf.Empty"
          ),
          ModelBuilder.Command(
            "com.lightbend.MyService.Get",
            "com.lightbend.GetValue",
            "com.lightbend.MyState"
          )
        ),
        List.empty
      )
    )
    val mainPackageName = "com.lightbend"
    val mainClassName   = "Main"

    val sourceDoc = SourceGenerator.mainSource(mainPackageName, mainClassName, entities)
    assertEquals(
      sourceDoc.layout,
      """package com.lightbend;
        |
        |import com.akkaserverless.javasdk.AkkaServerless;
        |import com.lightbend.something.MyEntity3;
        |import com.lightbend.something.MyService3;
        |
        |public final class Main {
        |    
        |    public static void main(String[] args) throws Exception {
        |        new AkkaServerless()
        |            .registerEventSourcedEntity(
        |                MyService1.class,
        |                MyEntity1.getDescriptor().findServiceByName("MyService1"),
        |                MyEntity1.getDescriptor()
        |            )
        |            .registerEventSourcedEntity(
        |                MyService2.class,
        |                MyEntity2.getDescriptor().findServiceByName("MyService2"),
        |                MyEntity2.getDescriptor()
        |            )
        |            .registerEventSourcedEntity(
        |                MyService3.class,
        |                MyEntity3.getDescriptor().findServiceByName("MyService3"),
        |                MyEntity3.getDescriptor()
        |            )
        |            .start().toCompletableFuture().get();
        |    }
        |    
        |}""".stripMargin
    )
  }

  test("main source with no outer class") {
    val entities = List(
      ModelBuilder.EventSourcedEntity(
        Some("com/lightbend"),
        None,
        "com.lightbend.MyService1",
        "MyService1",
        Some("com.lightbend.State1"),
        List(
          ModelBuilder.Command(
            "com.lightbend.MyService.Set",
            "com.lightbend.SetValue",
            "com.google.protobuf.Empty"
          ),
          ModelBuilder.Command(
            "com.lightbend.MyService.Get",
            "com.lightbend.GetValue",
            "com.lightbend.MyState"
          )
        ),
        List.empty
      )
    )
    val mainPackageName = "com.lightbend"
    val mainClassName   = "Main"

    val sourceDoc = SourceGenerator.mainSource(mainPackageName, mainClassName, entities)
    assertEquals(
      sourceDoc.layout,
      """package com.lightbend;
        |
        |import com.akkaserverless.javasdk.AkkaServerless;
        |
        |public final class Main {
        |    
        |    public static void main(String[] args) throws Exception {
        |        new AkkaServerless()
        |            // FIXME: No Java outer class name specified - cannot register MyService1 - ensure you are generating protobuf for Java
        |            .start().toCompletableFuture().get();
        |    }
        |    
        |}""".stripMargin
    )
  }
}
