/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package java

import org.apache.commons.io.FileUtils

import _root_.java.nio.file.Files

class SourceGeneratorSuite extends munit.FunSuite {
  def serviceProto(suffix: String = ""): PackageNaming =
    PackageNaming(
      s"MyService$suffix",
      "com.example.service",
      None,
      None,
      Some(s"ServiceOuterClass$suffix"),
      javaMultipleFiles = false
    )

  def domainProto(suffix: String = ""): PackageNaming =
    PackageNaming(
      s"Domain$suffix",
      "com.example.service.persistence",
      None,
      None,
      Some(s"EntityOuterClass$suffix"),
      javaMultipleFiles = false
    )

  val externalProto: PackageNaming =
    PackageNaming(
      "EXT",
      "com.external",
      None,
      None,
      None,
      javaMultipleFiles = true
    )

  def simpleService(
      proto: PackageNaming = serviceProto(),
      suffix: String = ""
  ): ModelBuilder.Service =
    ModelBuilder.Service(
      FullyQualifiedName(s"MyService$suffix", proto),
      s"com.example.Entity$suffix",
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

  def eventSourcedEntity(
      suffix: String = ""
  ): ModelBuilder.EventSourcedEntity =
    ModelBuilder.EventSourcedEntity(
      FullyQualifiedName(s"MyEntity$suffix", domainProto(suffix)),
      s"MyEntity$suffix",
      Some(ModelBuilder.State(FullyQualifiedName("MyState", domainProto(suffix)))),
      List(
        ModelBuilder.Event(FullyQualifiedName("SetEvent", domainProto(suffix)))
      )
    )

  def valueEntity(suffix: String = ""): ModelBuilder.ValueEntity =
    ModelBuilder.ValueEntity(
      FullyQualifiedName(s"MyValueEntity$suffix", domainProto(suffix)),
      s"MyValueEntity$suffix",
      ModelBuilder.State(FullyQualifiedName("MyState", domainProto(suffix)))
    )

  test("generate") {
    val sourceDirectory          = Files.createTempDirectory("source-generator-test")
    val generatedSourceDirectory = Files.createTempDirectory("generated-source-generator-test")
    try {

      val testSourceDirectory = Files.createTempDirectory("test-source-generator-test")

      try {

        val source1     = sourceDirectory.resolve("com/example/service/persistence/MyEntity1Impl.java")
        val sourceFile1 = source1.toFile
        FileUtils.forceMkdir(sourceFile1.getParentFile)
        FileUtils.touch(sourceFile1)

        val testSource2 =
          testSourceDirectory.resolve("com/example/service/persistence/MyValueEntity2Test.java")
        val testSourceFile2 = testSource2.toFile
        FileUtils.forceMkdir(testSourceFile2.getParentFile)
        FileUtils.touch(testSourceFile2)

        val implSource1 =
          generatedSourceDirectory.resolve(
            "com/example/service/persistence/MyEntity1Interface.java"
          )
        val implSourceFile1 = implSource1.toFile
        val implSource2 =
          generatedSourceDirectory.resolve(
            "com/example/service/persistence/MyEntity2Interface.java"
          )
        val implSourceFile2 = implSource2.toFile
        FileUtils.forceMkdir(implSourceFile1.getParentFile)
        FileUtils.touch(implSourceFile1)
        FileUtils.touch(implSourceFile2)

        val service1Proto = serviceProto("1")
        val service2Proto = serviceProto("2")
        val service3Proto = serviceProto("3").copy(pkg = "com.example.service.something")

        val services = Map(
          "com.example.Service1" -> simpleService(service1Proto, "1"),
          "com.example.Service2" -> simpleService(service2Proto, "2"),
          "com.example.Service3" -> simpleService(service3Proto, "3")
        )

        val entities = Map(
          "com.example.Entity1" -> eventSourcedEntity(suffix = "1"),
          "com.example.Entity2" -> valueEntity(suffix = "2"),
          "com.example.Entity3" -> eventSourcedEntity(suffix = "3")
        )

        val sources = SourceGenerator.generate(
          ModelBuilder.Model(services, entities),
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
            generatedSourceDirectory.resolve(
              "com/example/service/persistence/MyEntity1Interface.java"
            ),
            sourceDirectory.resolve("com/example/service/persistence/MyValueEntity2Impl.java"),
            generatedSourceDirectory.resolve(
              "com/example/service/persistence/MyValueEntity2Interface.java"
            ),
            sourceDirectory.resolve("com/example/service/persistence/MyEntity3Impl.java"),
            generatedSourceDirectory.resolve(
              "com/example/service/persistence/MyEntity3Interface.java"
            ),
            testSourceDirectory.resolve(
              "com/example/service/persistence/MyEntity3Test.java"
            ),
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

    val entity  = eventSourcedEntity()
    val service = simpleService()

    val packageName        = "com.example.service"
    val className          = "MyServiceEntityImpl"
    val interfaceClassName = "MyServiceEntityInterface"
    val entityType         = "MyServiceEntity"

    val sourceDoc =
      SourceGenerator.source(
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
      |@EventSourcedEntity(entityType = "MyServiceEntity")
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

    val service = simpleService()
    val entity  = valueEntity()

    val packageName        = "com.example.service"
    val className          = "MyServiceImpl"
    val interfaceClassName = "MyServiceInterface"
    val entityType         = "MyService"

    val sourceDoc =
      SourceGenerator.source(
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
      |@ValueEntity(entityType = "MyService")
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
    val service     = simpleService()
    val entity      = eventSourcedEntity()
    val packageName = "com.example.service"
    val className   = "MyServiceEntity"

    val sourceDoc = SourceGenerator.interfaceSource(service, entity, packageName, className)
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
      |    @Snapshot
      |    public abstract EntityOuterClass.MyState snapshot();
      |    
      |    @SnapshotHandler
      |    public abstract void handleSnapshot(EntityOuterClass.MyState snapshot);
      |    
      |    @CommandHandler(name = "set")
      |    public Reply<Empty> setWithReply(ServiceOuterClass.SetValue command, CommandContext ctx) {
      |        return Reply.message(set(command, ctx));
      |    }
      |    
      |    protected Empty set(ServiceOuterClass.SetValue command, CommandContext ctx) {
      |        return set(command);
      |    }
      |    
      |    protected Empty set(ServiceOuterClass.SetValue command) {
      |        return null;
      |    }
      |    
      |    @CommandHandler(name = "get")
      |    public Reply<ServiceOuterClass.MyState> getWithReply(ServiceOuterClass.GetValue command, CommandContext ctx) {
      |        return Reply.message(get(command, ctx));
      |    }
      |    
      |    protected ServiceOuterClass.MyState get(ServiceOuterClass.GetValue command, CommandContext ctx) {
      |        return get(command);
      |    }
      |    
      |    protected ServiceOuterClass.MyState get(ServiceOuterClass.GetValue command) {
      |        return null;
      |    }
      |    
      |    @EventHandler
      |    public abstract void setEvent(EntityOuterClass.SetEvent event);
      |}""".stripMargin
    )
  }

  test("ValueEntity interface source") {
    val service     = simpleService()
    val entity      = valueEntity()
    val packageName = "com.example.service"
    val className   = "MyService"

    val sourceDoc = SourceGenerator.interfaceSource(service, entity, packageName, className)
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
      |    @CommandHandler(name = "set")
      |    public Reply<Empty> setWithReply(ServiceOuterClass.SetValue command, CommandContext<EntityOuterClass.MyState> ctx) {
      |        return Reply.message(set(command, ctx));
      |    }
      |    
      |    protected Empty set(ServiceOuterClass.SetValue command, CommandContext<EntityOuterClass.MyState> ctx) {
      |        return set(command);
      |    }
      |    
      |    protected Empty set(ServiceOuterClass.SetValue command) {
      |        return null;
      |    }
      |    
      |    @CommandHandler(name = "get")
      |    public Reply<ServiceOuterClass.MyState> getWithReply(ServiceOuterClass.GetValue command, CommandContext<EntityOuterClass.MyState> ctx) {
      |        return Reply.message(get(command, ctx));
      |    }
      |    
      |    protected ServiceOuterClass.MyState get(ServiceOuterClass.GetValue command, CommandContext<EntityOuterClass.MyState> ctx) {
      |        return get(command);
      |    }
      |    
      |    protected ServiceOuterClass.MyState get(ServiceOuterClass.GetValue command) {
      |        return null;
      |    }
      |}""".stripMargin
    )
  }

  test("EventSourcedEntity test source") {
    val service = simpleService()
    val entity  = eventSourcedEntity()

    val packageName   = "com.example.service"
    val implClassName = "MyServiceEntityImpl"
    val testClassName = "MyServiceEntityTest"

    val sourceDoc =
      SourceGenerator.testSource(service, entity, packageName, implClassName, testClassName)
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
    val service = simpleService()
    val entity  = valueEntity()

    val packageName   = "com.example.service"
    val implClassName = "MyServiceImpl"
    val testClassName = "MyServiceTest"

    val sourceDoc =
      SourceGenerator.testSource(service, entity, packageName, implClassName, testClassName)
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.valueentity.CommandContext;
        |import com.external.Empty;
        |import org.junit.Test;
        |import org.mockito.*;
        |
        |import static org.junit.Assert.assertThrows;
        |
        |public class MyServiceTest {
        |    private String entityId = "entityId1";
        |    private MyServiceImpl entity;
        |    private CommandContext context = Mockito.mock(CommandContext.class);
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

  test("main source") {
    val service1Proto = serviceProto("1")
    val service2Proto = serviceProto("2")
    val service3Proto = serviceProto("3").copy(pkg = "com.example.service.something")

    val services = Map(
      "com.example.Service1" -> simpleService(service1Proto, "1"),
      "com.example.Service2" -> simpleService(service2Proto, "2"),
      "com.example.Service3" -> simpleService(service3Proto, "3")
    )

    val entities = Map(
      "com.example.Entity1" -> eventSourcedEntity(suffix = "1"),
      "com.example.Entity2" -> valueEntity(suffix = "2"),
      "com.example.Entity3" -> eventSourcedEntity(suffix = "3")
    )

    val mainPackageName = "com.example.service"
    val mainClassName   = "Main"

    val sourceDoc = SourceGenerator.mainSource(
      mainPackageName,
      mainClassName,
      ModelBuilder.Model(services, entities)
    )
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.AkkaServerless;
        |import com.example.service.persistence.EntityOuterClass1;
        |import com.example.service.persistence.MyEntity1Impl;
        |import com.example.service.persistence.EntityOuterClass2;
        |import com.example.service.persistence.MyValueEntity2Impl;
        |import com.example.service.persistence.EntityOuterClass3;
        |import com.example.service.persistence.MyEntity3Impl;
        |
        |public final class Main {
        |    
        |    public static void main(String[] args) throws Exception {
        |        new AkkaServerless()
        |            .registerEventSourcedEntity(
        |                MyEntity1Impl.class,
        |                EntityOuterClass1.getDescriptor().findServiceByName("MyService1"),
        |                EntityOuterClass1.getDescriptor()
        |            )
        |            .registerEventSourcedEntity(
        |                MyValueEntity2Impl.class,
        |                EntityOuterClass2.getDescriptor().findServiceByName("MyService2"),
        |                EntityOuterClass2.getDescriptor()
        |            )
        |            .registerEventSourcedEntity(
        |                MyEntity3Impl.class,
        |                EntityOuterClass3.getDescriptor().findServiceByName("MyService3"),
        |                EntityOuterClass3.getDescriptor()
        |            )
        |            .start().toCompletableFuture().get();
        |    }
        |    
        |}""".stripMargin
    )
  }

  test("main source with no outer class") {
    val services = Map(
      "com.example.Service1" -> simpleService(suffix = "1")
    )

    val entities = Map(
      "com.example.Entity1" -> eventSourcedEntity("1").copy(fqn =
        FullyQualifiedName(s"MyEntity1", domainProto("1").copy(javaOuterClassnameOption = None))
      )
    )

    val mainPackageName = "com.example.service"
    val mainClassName   = "Main"

    val sourceDoc = SourceGenerator.mainSource(
      mainPackageName,
      mainClassName,
      ModelBuilder.Model(services, entities)
    )
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.AkkaServerless;
        |import com.example.service.persistence.MyEntity1Impl;
        |
        |public final class Main {
        |    
        |    public static void main(String[] args) throws Exception {
        |        new AkkaServerless()
        |            // FIXME: No Java outer class name specified - cannot register MyEntity1Impl - ensure you are generating protobuf for Java
        |            .start().toCompletableFuture().get();
        |    }
        |    
        |}""".stripMargin
    )
  }
}
