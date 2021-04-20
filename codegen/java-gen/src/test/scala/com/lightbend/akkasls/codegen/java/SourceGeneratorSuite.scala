/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package java

import org.apache.commons.io.FileUtils

import _root_.java.nio.file.Files

class SourceGeneratorSuite extends munit.FunSuite {
  def serviceProto(suffix: String = "") =
    PackageNaming(
      s"MyService$suffix",
      "com.example.service",
      None,
      None,
      Some(s"OuterClass$suffix"),
      false
    )

  val domainProto =
    PackageNaming(
      "Domain",
      "com.example.service.persistence",
      None,
      None,
      None,
      false
    )

  val externalProto =
    PackageNaming(
      "EXT",
      "com.external",
      None,
      None,
      None,
      true
    )

  def eventSourcedEntityService(proto: PackageNaming = serviceProto(), suffix: String = "") =
    ModelBuilder.Service(
      FullyQualifiedName(s"MyService$suffix", proto),
      ModelBuilder.EventSourcedEntity(
        FullyQualifiedName(s"MyEntity$suffix", domainProto),
        s"MyEntity$suffix",
        Some(ModelBuilder.State(FullyQualifiedName("MyState", domainProto))),
        List(
          ModelBuilder.Event(FullyQualifiedName("SetEvent", domainProto))
        )
      ),
      List(
        ModelBuilder.Command(
          FullyQualifiedName("Set", proto),
          FullyQualifiedName("SetValue", proto),
          FullyQualifiedName("Empty", externalProto)
        ),
        ModelBuilder.Command(
          FullyQualifiedName("Get", proto),
          FullyQualifiedName("GetValue", proto),
          FullyQualifiedName("MyState", proto)
        )
      )
    )

  def valueEntityService(proto: PackageNaming = serviceProto(), suffix: String = "") =
    ModelBuilder.Service(
      FullyQualifiedName(s"MyService$suffix", proto),
      ModelBuilder.ValueEntity(
        FullyQualifiedName(s"MyValueEntity$suffix", domainProto),
        s"MyValueEntity$suffix",
        Some(ModelBuilder.State(FullyQualifiedName("MyState", domainProto)))
      ),
      List(
        ModelBuilder.Command(
          FullyQualifiedName("Set", proto),
          FullyQualifiedName("SetValue", proto),
          FullyQualifiedName("Empty", externalProto)
        ),
        ModelBuilder.Command(
          FullyQualifiedName("Get", proto),
          FullyQualifiedName("GetValue", proto),
          FullyQualifiedName("MyState", proto)
        )
      )
    )

  test("generate") {
    val sourceDirectory          = Files.createTempDirectory("source-generator-test")
    val generatedSourceDirectory = Files.createTempDirectory("generated-source-generator-test")
    try {

      val testSourceDirectory = Files.createTempDirectory("test-source-generator-test")

      try {

        val source1     = sourceDirectory.resolve("com/example/service/MyService1Impl.java")
        val sourceFile1 = source1.toFile
        FileUtils.forceMkdir(sourceFile1.getParentFile)
        FileUtils.touch(sourceFile1)

        val testSource2     = testSourceDirectory.resolve("com/example/service/MyService2Test.java")
        val testSourceFile2 = testSource2.toFile
        FileUtils.forceMkdir(testSourceFile2.getParentFile)
        FileUtils.touch(testSourceFile2)

        val implSource1 =
          generatedSourceDirectory.resolve("com/example/service/MyService1.java")
        val implSourceFile1 = implSource1.toFile
        val implSource2 =
          generatedSourceDirectory.resolve("com/example/service/MyService2.java")
        val implSourceFile2 = implSource2.toFile
        FileUtils.forceMkdir(implSourceFile1.getParentFile)
        FileUtils.touch(implSourceFile1)
        FileUtils.touch(implSourceFile2)

        val service1Proto = serviceProto("1")
        val service2Proto = serviceProto("2")
        val service3Proto = serviceProto("3").copy(pkg = "com.example.service.something")

        val entities = List(
          eventSourcedEntityService(service1Proto, "1"),
          valueEntityService(service2Proto, "2"),
          eventSourcedEntityService(service3Proto, "3")
        )

        val sources = SourceGenerator.generate(
          entities,
          sourceDirectory,
          testSourceDirectory,
          generatedSourceDirectory,
          "com.example.service.Main"
        )

        assertEquals(Files.size(source1), 0L)
        assertEquals(Files.size(testSource2), 0L)

        assertEquals(
          sources,
          List(
            generatedSourceDirectory.resolve("com/example/service/MyService1.java"),
            sourceDirectory.resolve("com/example/service/MyService2Impl.java"),
            generatedSourceDirectory.resolve("com/example/service/MyService2.java"),
            sourceDirectory.resolve("com/example/service/something/MyService3Impl.java"),
            generatedSourceDirectory.resolve("com/example/service/something/MyService3.java"),
            testSourceDirectory.resolve("com/example/service/something/MyService3Test.java"),
            sourceDirectory.resolve("com/example/service/Main.java")
          )
        )

        // Test that the main, source and test files are being written to
        assertEquals(Files.readAllBytes(sources.head).head.toChar, 'p')
        assertEquals(Files.readAllBytes(sources.drop(1).head).head.toChar, 'p')
        assertEquals(Files.readAllBytes(sources.drop(3).head).head.toChar, 'p')

      } finally FileUtils.deleteDirectory(testSourceDirectory.toFile)
    } finally FileUtils.deleteDirectory(sourceDirectory.toFile)
  }

  test("EventSourcedEntity source") {

    val serviceProto =
      PackageNaming(
        "MyService",
        "com.example.service",
        None,
        None,
        Some("OuterClass"),
        false
      )

    val domainProto =
      PackageNaming(
        "Domain",
        "com.example.service.persistence",
        None,
        None,
        None,
        false
      )

    val entity = eventSourcedEntityService()

    val packageName        = "com.example.service"
    val className          = "MyServiceEntityImpl"
    val interfaceClassName = "MyServiceEntity"

    val sourceDoc = SourceGenerator.source(entity, packageName, className, interfaceClassName)
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
      |public class MyServiceEntityImpl extends MyServiceEntity {
      |    @SuppressWarnings("unused")
      |    private final String entityId;
      |    
      |    public MyServiceEntityImpl(@EntityId String entityId) {
      |        this.entityId = entityId;
      |    }
      |    
      |    @Override
      |    public Domain.MyState snapshot() {
      |        // TODO: produce state snapshot here
      |        return Domain.MyState.newBuilder().build();
      |    }
      |    
      |    @Override
      |    public void handleSnapshot(Domain.MyState snapshot) {
      |        // TODO: restore state from snapshot here
      |        
      |    }
      |    
      |    @Override
      |    public Empty set(OuterClass.SetValue command, CommandContext ctx) {
      |        throw ctx.fail("The command handler for `Set` is not implemented, yet");
      |    }
      |    
      |    @Override
      |    public OuterClass.MyState get(OuterClass.GetValue command, CommandContext ctx) {
      |        throw ctx.fail("The command handler for `Get` is not implemented, yet");
      |    }
      |    
      |    @Override
      |    public void setEvent(Domain.SetEvent event) {
      |        throw new RuntimeException("The event handler for `SetEvent` is not implemented, yet");
      |    }
      |}""".stripMargin
    )
  }

  test("ValueEntity source") {

    val serviceProto =
      PackageNaming(
        "MyService",
        "com.example.service",
        None,
        None,
        Some("OuterClass"),
        false
      )

    val domainProto =
      PackageNaming(
        "Domain",
        "com.example.service.persistence",
        None,
        None,
        None,
        false
      )

    val entity = valueEntityService()

    val packageName        = "com.example.service"
    val className          = "MyServiceImpl"
    val interfaceClassName = "MyService"

    val sourceDoc = SourceGenerator.source(entity, packageName, className, interfaceClassName)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
      |
      |import com.akkaserverless.javasdk.EntityId;
      |import com.akkaserverless.javasdk.valueentity.*;
      |import com.example.service.persistence.Domain;
      |import com.external.Empty;
      |
      |/** A value entity. */
      |@ValueEntity(entityType = "MyService")
      |public class MyServiceImpl extends MyService {
      |    @SuppressWarnings("unused")
      |    private final String entityId;
      |    
      |    public MyServiceImpl(@EntityId String entityId) {
      |        this.entityId = entityId;
      |    }
      |    
      |    @Override
      |    public Empty set(OuterClass.SetValue command, CommandContext ctx) {
      |        throw ctx.fail("The command handler for `Set` is not implemented, yet");
      |    }
      |    
      |    @Override
      |    public OuterClass.MyState get(OuterClass.GetValue command, CommandContext ctx) {
      |        throw ctx.fail("The command handler for `Get` is not implemented, yet");
      |    }
      |}""".stripMargin
    )
  }

  test("EventSourcedEntity interface source") {
    val entity      = eventSourcedEntityService()
    val packageName = "com.example.service"
    val className   = "MyServiceEntity"

    val sourceDoc = SourceGenerator.interfaceSource(entity, packageName, className)
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
      |public abstract class MyServiceEntity {
      |    @Snapshot
      |    public abstract Domain.MyState snapshot();
      |    
      |    @SnapshotHandler
      |    public abstract void handleSnapshot(Domain.MyState snapshot);
      |    
      |    @CommandHandler
      |    public abstract Empty set(OuterClass.SetValue command, CommandContext ctx);
      |    
      |    @CommandHandler
      |    public abstract OuterClass.MyState get(OuterClass.GetValue command, CommandContext ctx);
      |    
      |    @EventHandler
      |    public abstract void setEvent(Domain.SetEvent event);
      |}""".stripMargin
    )
  }

  test("ValueEntity interface source") {
    val entity      = valueEntityService()
    val packageName = "com.example.service"
    val className   = "MyService"

    val sourceDoc = SourceGenerator.interfaceSource(entity, packageName, className)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
      |
      |import com.akkaserverless.javasdk.EntityId;
      |import com.akkaserverless.javasdk.valueentity.*;
      |import com.example.service.persistence.Domain;
      |import com.external.Empty;
      |
      |/** A value entity. */
      |public abstract class MyService {
      |    @CommandHandler
      |    public abstract Empty set(OuterClass.SetValue command, CommandContext ctx);
      |    
      |    @CommandHandler
      |    public abstract OuterClass.MyState get(OuterClass.GetValue command, CommandContext ctx);
      |}""".stripMargin
    )
  }

  test("EventSourcedEntity test source") {
    val entity = eventSourcedEntityService()

    val packageName   = "com.example.service"
    val implClassName = "MyServiceEntityImpl"
    val testClassName = "MyServiceEntityTest"

    val sourceDoc = SourceGenerator.testSource(entity, packageName, implClassName, testClassName)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.eventsourcedentity.CommandContext;
        |import com.external.Empty;
        |import org.junit.Test;
        |import org.mockito.*;
        |
        |public class MyServiceEntityTest {
        |    private String entityId = "entityId1";
        |    private MyServiceEntityImpl entity;
        |    private CommandContext context = Mockito.mock(CommandContext.class);
        |    
        |    @Test
        |    public void setTest() {
        |        entity = new MyServiceEntityImpl(entityId);
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
        |        entity = new MyServiceEntityImpl(entityId);
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

  test("ValueEntity test source") {
    val entity = valueEntityService()

    val packageName   = "com.example.service"
    val implClassName = "MyServiceImpl"
    val testClassName = "MyServiceTest"

    val sourceDoc = SourceGenerator.testSource(entity, packageName, implClassName, testClassName)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.valueentity.CommandContext;
        |import com.external.Empty;
        |import org.junit.Test;
        |import org.mockito.*;
        |
        |public class MyServiceTest {
        |    private String entityId = "entityId1";
        |    private MyServiceImpl entity;
        |    private CommandContext context = Mockito.mock(CommandContext.class);
        |    
        |    @Test
        |    public void setTest() {
        |        entity = new MyServiceImpl(entityId);
        |        
        |        // TODO: you may want to set fields in addition to the entity id
        |        //    entity.set(OuterClass.SetValue.newBuilder().setEntityId(entityId).build(), context);
        |    }
        |    
        |    @Test
        |    public void getTest() {
        |        entity = new MyServiceImpl(entityId);
        |        
        |        // TODO: you may want to set fields in addition to the entity id
        |        //    entity.get(OuterClass.GetValue.newBuilder().setEntityId(entityId).build(), context);
        |    }
        |}""".stripMargin
    )
  }

  test("main source") {
    val service1Proto = serviceProto("1")
    val service2Proto = serviceProto("2")
    val service3Proto = serviceProto("3").copy(pkg = "com.example.service.something")

    val entities = List(
      eventSourcedEntityService(service1Proto, "1"),
      valueEntityService(service2Proto, "2"),
      eventSourcedEntityService(service3Proto, "3")
    )

    val mainPackageName = "com.example.service"
    val mainClassName   = "Main"

    val sourceDoc = SourceGenerator.mainSource(mainPackageName, mainClassName, entities)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.AkkaServerless;
        |import com.example.service.something.OuterClass3;
        |import com.example.service.something.MyService3Impl;
        |
        |public final class Main {
        |    
        |    public static void main(String[] args) throws Exception {
        |        new AkkaServerless()
        |            .registerEventSourcedEntity(
        |                MyService1Impl.class,
        |                OuterClass1.getDescriptor().findServiceByName("MyService1"),
        |                OuterClass1.getDescriptor()
        |            )
        |            .registerEventSourcedEntity(
        |                MyService2Impl.class,
        |                OuterClass2.getDescriptor().findServiceByName("MyService2"),
        |                OuterClass2.getDescriptor()
        |            )
        |            .registerEventSourcedEntity(
        |                MyService3Impl.class,
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
    val entity = eventSourcedEntityService(
      serviceProto().copy(javaOuterClassnameOption = None)
    )

    val mainPackageName = "com.example.service"
    val mainClassName   = "Main"

    val sourceDoc = SourceGenerator.mainSource(mainPackageName, mainClassName, List(entity))
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.AkkaServerless;
        |
        |public final class Main {
        |    
        |    public static void main(String[] args) throws Exception {
        |        new AkkaServerless()
        |            // FIXME: No Java outer class name specified - cannot register MyService - ensure you are generating protobuf for Java
        |            .start().toCompletableFuture().get();
        |    }
        |    
        |}""".stripMargin
    )
  }
}
