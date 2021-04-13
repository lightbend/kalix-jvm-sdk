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

        val service1Proto =
          ModelBuilder.ProtoReference(
            "service.proto",
            "com.example.service",
            None,
            None,
            Some("OuterClass1")
          )

        val service2Proto =
          ModelBuilder.ProtoReference(
            "service.proto",
            "com.example.service",
            None,
            None,
            Some("OuterClass2")
          )

        val service3Proto =
          ModelBuilder.ProtoReference(
            "service.proto",
            "com.example.service",
            None,
            None,
            Some("OuterClass3")
          )

        val domainProto =
          ModelBuilder.ProtoReference(
            "persistence/domain.proto",
            "com.example.service.persistence",
            None,
            None,
            Some("Domain")
          )

        val entities = List(
          ModelBuilder.EventSourcedEntity(
            service1Proto,
            "com.lightbend.MyService1",
            "MyService1",
            Some(ModelBuilder.TypeReference("MyState", domainProto)),
            List(
              ModelBuilder.Command(
                "com.lightbend.MyService.Set",
                ModelBuilder.TypeReference("SetValue", service1Proto),
                ModelBuilder.TypeReference("protobuf.Empty", service1Proto)
              ),
              ModelBuilder.Command(
                "com.lightbend.MyService.Get",
                ModelBuilder.TypeReference("GetValue", service1Proto),
                ModelBuilder.TypeReference("MyState", service1Proto)
              )
            ),
            List.empty
          ),
          ModelBuilder.EventSourcedEntity(
            service2Proto,
            "com.lightbend.MyService2",
            "MyService2",
            Some(ModelBuilder.TypeReference("MyState2", domainProto)),
            List(
              ModelBuilder.Command(
                "com.lightbend.MyService.Set",
                ModelBuilder.TypeReference("SetValue", service2Proto),
                ModelBuilder.TypeReference("protobuf.Empty", service2Proto)
              ),
              ModelBuilder.Command(
                "com.lightbend.MyService.Get",
                ModelBuilder.TypeReference("GetValue", service2Proto),
                ModelBuilder.TypeReference("MyState", service2Proto)
              )
            ),
            List.empty
          ),
          ModelBuilder.EventSourcedEntity(
            service3Proto,
            "com.lightbend.something.MyService3",
            "MyService3",
            Some(ModelBuilder.TypeReference("MyState3", domainProto)),
            List(
              ModelBuilder.Command(
                "com.lightbend.MyService.Set",
                ModelBuilder.TypeReference("SetValue", service3Proto),
                ModelBuilder.TypeReference("protobuf.Empty", service3Proto)
              ),
              ModelBuilder.Command(
                "com.lightbend.MyService.Get",
                ModelBuilder.TypeReference("GetValue", service3Proto),
                ModelBuilder.TypeReference("MyState", service3Proto)
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

    val serviceProto =
      ModelBuilder.ProtoReference(
        "service.proto",
        "com.example.service",
        None,
        None,
        Some("OuterClass")
      )

    val domainProto =
      ModelBuilder.ProtoReference(
        "persistence/domain.proto",
        "com.example.service.persistence",
        None,
        None,
        Some("Domain")
      )

    val externalProto =
      ModelBuilder.ProtoReference(
        "com.external.imported.types",
        "com.external",
        None,
        None,
        None
      )

    val entity = ModelBuilder.EventSourcedEntity(
      serviceProto,
      "com.lightbend.MyServiceEntity",
      "MyServiceEntity",
      Some(ModelBuilder.TypeReference("MyState", domainProto)),
      List(
        ModelBuilder.Command(
          "com.lightbend.MyService.Set",
          ModelBuilder.TypeReference("SetValue", serviceProto),
          ModelBuilder.TypeReference("Empty", externalProto)
        ),
        ModelBuilder.Command(
          "com.lightbend.MyService.Get",
          ModelBuilder.TypeReference("GetValue", serviceProto),
          ModelBuilder.TypeReference("MyState", serviceProto)
        )
      ),
      List(
        ModelBuilder.TypeReference("SetEvent", domainProto)
      )
    )

    val packageName = "com.example.service"
    val className   = "MyServiceEntity"

    val sourceDoc = SourceGenerator.source(entity, packageName, className)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
      |
      |import com.akkaserverless.javasdk.EntityId;
      |import com.akkaserverless.javasdk.eventsourcedentity.*;
      |import com.example.service.persistence.Domain;
      |import com.external.Empty;
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
      |    public Domain.MyState snapshot() {
      |        // TODO: produce state snapshot here
      |        return Domain.MyState.newBuilder().build();
      |    }
      |    
      |    @SnapshotHandler
      |    public void handleSnapshot(Domain.MyState snapshot) {
      |        // TODO: restore state from snapshot here
      |        
      |    }
      |    
      |    @CommandHandler
      |    public Empty set(OuterClass.SetValue command, CommandContext ctx) {
      |        throw new RuntimeException("The command handler for `Set` is not implemented, yet");
      |    }
      |    
      |    @CommandHandler
      |    public OuterClass.MyState get(OuterClass.GetValue command, CommandContext ctx) {
      |        throw new RuntimeException("The command handler for `Get` is not implemented, yet");
      |    }
      |    
      |    @EventHandler
      |    public void setEvent(Domain.SetEvent event) {
      |        throw new RuntimeException("The event handler for `SetEvent` is not implemented, yet");
      |    }
      |}""".stripMargin
    )
  }

  test("test source") {
    val serviceProto =
      ModelBuilder.ProtoReference(
        "service.proto",
        "com.example.service",
        None,
        None,
        Some("OuterClass")
      )

    val domainProto =
      ModelBuilder.ProtoReference(
        "persistence/domain.proto",
        "com.example.service.persistence",
        None,
        None,
        Some("Domain")
      )

    val entity = ModelBuilder.EventSourcedEntity(
      serviceProto,
      "com.lightbend.MyServiceEntity",
      "MyServiceEntity",
      Some(ModelBuilder.TypeReference("MyState", domainProto)),
      List(
        ModelBuilder.Command(
          "com.lightbend.MyService.Set",
          ModelBuilder.TypeReference("SetValue", serviceProto),
          ModelBuilder.TypeReference("protobuf.Empty", serviceProto)
        ),
        ModelBuilder.Command(
          "com.lightbend.MyService.Get",
          ModelBuilder.TypeReference("GetValue", serviceProto),
          ModelBuilder.TypeReference("MyState", serviceProto)
        )
      ),
      List.empty
    )

    val packageName   = "com.example.service"
    val className     = "MyServiceEntity"
    val testClassName = "MyServiceEntityTest"

    val sourceDoc = SourceGenerator.testSource(entity, packageName, className, testClassName)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
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
        |        //    entity.set(OuterClass.SetValue.newBuilder().setEntityId(entityId).build(), context);
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
        |        //    entity.get(OuterClass.GetValue.newBuilder().setEntityId(entityId).build(), context);
        |        
        |        // TODO: if you wish to verify events:
        |        //    Mockito.verify(context).emit(event);
        |    }
        |}""".stripMargin
    )
  }

  test("main source") {
    val service1Proto =
      ModelBuilder.ProtoReference(
        "service.proto",
        "com.example.service",
        None,
        None,
        Some("OuterClass1")
      )

    val service2Proto =
      ModelBuilder.ProtoReference(
        "service.proto",
        "com.example.service",
        None,
        None,
        Some("OuterClass2")
      )

    val service3Proto =
      ModelBuilder.ProtoReference(
        "service.proto",
        "com.example.service",
        None,
        None,
        Some("OuterClass3")
      )

    val domainProto =
      ModelBuilder.ProtoReference(
        "persistence/domain.proto",
        "com.example.service.persistence",
        None,
        Some("com.lightbend.something"),
        Some("Domain")
      )

    val entities = List(
      ModelBuilder.EventSourcedEntity(
        service1Proto,
        "com.lightbend.MyService1",
        "MyService1",
        Some(ModelBuilder.TypeReference("MyState", domainProto)),
        List(
          ModelBuilder.Command(
            "com.lightbend.MyService.Set",
            ModelBuilder.TypeReference("SetValue", service1Proto),
            ModelBuilder.TypeReference("protobuf.Empty", service1Proto)
          ),
          ModelBuilder.Command(
            "com.lightbend.MyService.Get",
            ModelBuilder.TypeReference("GetValue", service1Proto),
            ModelBuilder.TypeReference("MyState", service1Proto)
          )
        ),
        List.empty
      ),
      ModelBuilder.EventSourcedEntity(
        service2Proto,
        "com.lightbend.MyService2",
        "MyService2",
        Some(ModelBuilder.TypeReference("MyState2", domainProto)),
        List(
          ModelBuilder.Command(
            "com.lightbend.MyService.Set",
            ModelBuilder.TypeReference("SetValue", service2Proto),
            ModelBuilder.TypeReference("protobuf.Empty", service2Proto)
          ),
          ModelBuilder.Command(
            "com.lightbend.MyService.Get",
            ModelBuilder.TypeReference("GetValue", service2Proto),
            ModelBuilder.TypeReference("MyState", service2Proto)
          )
        ),
        List.empty
      ),
      ModelBuilder.EventSourcedEntity(
        service3Proto,
        "com.lightbend.something.MyService3",
        "MyService3",
        Some(ModelBuilder.TypeReference("MyState3", domainProto)),
        List(
          ModelBuilder.Command(
            "com.lightbend.MyService.Set",
            ModelBuilder.TypeReference("SetValue", service3Proto),
            ModelBuilder.TypeReference("protobuf.Empty", service3Proto)
          ),
          ModelBuilder.Command(
            "com.lightbend.MyService.Get",
            ModelBuilder.TypeReference("GetValue", service3Proto),
            ModelBuilder.TypeReference("MyState", service3Proto)
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
        |import com.lightbend.something.OuterClass3;
        |import com.lightbend.something.MyService3;
        |
        |public final class Main {
        |    
        |    public static void main(String[] args) throws Exception {
        |        new AkkaServerless()
        |            .registerEventSourcedEntity(
        |                MyService1.class,
        |                OuterClass1.getDescriptor().findServiceByName("MyService1"),
        |                OuterClass1.getDescriptor()
        |            )
        |            .registerEventSourcedEntity(
        |                MyService2.class,
        |                OuterClass2.getDescriptor().findServiceByName("MyService2"),
        |                OuterClass2.getDescriptor()
        |            )
        |            .registerEventSourcedEntity(
        |                MyService3.class,
        |                OuterClass3.getDescriptor().findServiceByName("MyService3"),
        |                OuterClass3.getDescriptor()
        |            )
        |            .start().toCompletableFuture().get();
        |    }
        |    
        |}""".stripMargin
    )
  }

  test("main source with no outer class") {
    val serviceProto =
      ModelBuilder.ProtoReference(
        "service.proto",
        "com.example.service",
        None,
        None,
        None
      )

    val domainProto =
      ModelBuilder.ProtoReference(
        "persistence/domain.proto",
        "com.example.service.persistence",
        None,
        None,
        Some("Domain")
      )

    val entity = ModelBuilder.EventSourcedEntity(
      serviceProto,
      "com.lightbend.MyServiceEntity",
      "MyServiceEntity",
      Some(ModelBuilder.TypeReference("MyState", domainProto)),
      List(
        ModelBuilder.Command(
          "com.lightbend.MyService.Set",
          ModelBuilder.TypeReference("SetValue", serviceProto),
          ModelBuilder.TypeReference("protobuf.Empty", serviceProto)
        ),
        ModelBuilder.Command(
          "com.lightbend.MyService.Get",
          ModelBuilder.TypeReference("GetValue", serviceProto),
          ModelBuilder.TypeReference("MyState", serviceProto)
        )
      ),
      List.empty
    )

    val mainPackageName = "com.lightbend"
    val mainClassName   = "Main"

    val sourceDoc = SourceGenerator.mainSource(mainPackageName, mainClassName, List(entity))
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
        |            // FIXME: No Java outer class name specified - cannot register MyServiceEntity - ensure you are generating protobuf for Java
        |            .start().toCompletableFuture().get();
        |    }
        |    
        |}""".stripMargin
    )
  }
}
