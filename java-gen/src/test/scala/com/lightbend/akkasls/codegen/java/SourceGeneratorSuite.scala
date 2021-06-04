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
        val integrationTestSourceDirectory =
          Files.createTempDirectory("integration-test-source-generator-test")
        try {
          val generatedSourceDirectory =
            Files.createTempDirectory("generated-source-generator-test")
          try {

            val source1 =
              sourceDirectory.resolve("com/example/service/persistence/MyEntity1Impl.java")
            val sourceFile1 = source1.toFile
            FileUtils.forceMkdir(sourceFile1.getParentFile)
            FileUtils.touch(sourceFile1)

            val testSource2 =
              testSourceDirectory.resolve("com/example/service/persistence/MyValueEntity2Test.java")
            val testSourceFile2 = testSource2.toFile
            FileUtils.forceMkdir(testSourceFile2.getParentFile)
            FileUtils.touch(testSourceFile2)

            val integrationTestSource2 =
              integrationTestSourceDirectory.resolve(
                "com/example/service/persistence/MyValueEntity2IntegrationTest.java"
              )
            val integrationTestSourceFile2 = integrationTestSource2.toFile
            FileUtils.forceMkdir(integrationTestSourceFile2.getParentFile)
            FileUtils.touch(integrationTestSourceFile2)

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

            val service1Proto = TestData.serviceProto("1")
            val service2Proto = TestData.serviceProto("2")
            val service3Proto =
              TestData.serviceProto("3").copy(pkg = "com.example.service.something")
            val service4Proto = TestData.serviceProto("4")

            val services = Map(
              "com.example.Service1" -> TestData.simpleEntityService(service1Proto, "1"),
              "com.example.Service2" -> TestData.simpleEntityService(service2Proto, "2"),
              "com.example.Service3" -> TestData.simpleEntityService(service3Proto, "3"),
              "com.example.Service4" -> TestData.simpleViewService(service4Proto, "4")
            )

            val entities = Map(
              "com.example.Entity1" -> TestData.eventSourcedEntity(suffix = "1"),
              "com.example.Entity2" -> TestData.valueEntity(suffix = "2"),
              "com.example.Entity3" -> TestData.eventSourcedEntity(suffix = "3")
            )

            val sources = SourceGenerator.generate(
              ModelBuilder.Model(services, entities),
              sourceDirectory,
              testSourceDirectory,
              integrationTestSourceDirectory,
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
                integrationTestSourceDirectory.resolve(
                  "com/example/service/persistence/MyEntity3IntegrationTest.java"
                ),
                sourceDirectory.resolve("com/example/service/MyService4Impl.java"),
                generatedSourceDirectory.resolve(
                  "com/example/service/MyService4Interface.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/MainComponentRegistrations.java"
                ),
                sourceDirectory.resolve("com/example/service/Main.java")
              )
            )

            // Test that the main, source and test files are being written to
            assertEquals(Files.readAllBytes(sources.head).head.toChar, 'p')
            assertEquals(Files.readAllBytes(sources.drop(1).head).head.toChar, 'p')
            assertEquals(Files.readAllBytes(sources.drop(3).head).head.toChar, 'p')

          } finally FileUtils.deleteDirectory(generatedSourceDirectory.toFile)
        } finally FileUtils.deleteDirectory(integrationTestSourceDirectory.toFile)
      } finally FileUtils.deleteDirectory(testSourceDirectory.toFile)
    } finally FileUtils.deleteDirectory(sourceDirectory.toFile)
  }

  test("generated main source") {
    val service1Proto = TestData.serviceProto("1")
    val service2Proto = TestData.serviceProto("2")
    val service3Proto = TestData.serviceProto("3").copy(pkg = "com.example.service.something")
    val service4Proto = TestData.serviceProto("4").copy(pkg = "com.example.service.view")

    val services = Map(
      "com.example.Service1" -> TestData.simpleEntityService(service1Proto, "1"),
      "com.example.Service2" -> TestData.simpleEntityService(service2Proto, "2"),
      "com.example.Service3" -> TestData.simpleEntityService(service3Proto, "3"),
      "com.example.Service4" -> TestData.simpleViewService(service4Proto, "4")
    )

    val entities = Map(
      "com.example.Entity1" -> TestData.eventSourcedEntity(suffix = "1"),
      "com.example.Entity2" -> TestData.valueEntity(suffix = "2"),
      "com.example.Entity3" -> TestData.eventSourcedEntity(suffix = "3")
    )

    val mainPackageName = "com.example.service"
    val mainClassName   = "SomeMain"

    val sourceDoc = SourceGenerator.mainComponentRegistrationsSource(
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
        |import com.example.service.persistence.EntityOuterClass2;
        |import com.example.service.persistence.EntityOuterClass3;
        |import com.example.service.persistence.EntityOuterClass4;
        |import com.example.service.persistence.MyEntity1Impl;
        |import com.example.service.persistence.MyEntity3Impl;
        |import com.example.service.persistence.MyValueEntity2Impl;
        |import com.example.service.something.ServiceOuterClass3;
        |import com.example.service.view.MyService4Impl;
        |import com.example.service.view.ServiceOuterClass4;
        |import com.external.ExternalDomain;
        |
        |public final class SomeMainComponentRegistrations {
        |    
        |    public static AkkaServerless withGeneratedComponentsAdded(AkkaServerless akkaServerless) {
        |        return akkaServerless
        |                .registerEventSourcedEntity(
        |                    MyEntity1Impl.class,
        |                    ServiceOuterClass1.getDescriptor().findServiceByName("MyService1"),
        |                    EntityOuterClass1.getDescriptor(),
        |                    ExternalDomain.getDescriptor()
        |                )
        |                .registerValueEntity(
        |                    MyValueEntity2Impl.class,
        |                    ServiceOuterClass2.getDescriptor().findServiceByName("MyService2"),
        |                    EntityOuterClass2.getDescriptor(),
        |                    ExternalDomain.getDescriptor()
        |                )
        |                .registerEventSourcedEntity(
        |                    MyEntity3Impl.class,
        |                    ServiceOuterClass3.getDescriptor().findServiceByName("MyService3"),
        |                    EntityOuterClass3.getDescriptor(),
        |                    ExternalDomain.getDescriptor()
        |                )
        |                .registerView(
        |                    MyService4Impl.class,
        |                    ServiceOuterClass4.getDescriptor().findServiceByName("MyService4"),
        |                    "my-view-id4",
        |                    EntityOuterClass4.getDescriptor()
        |                );
        |    }
        |}""".stripMargin
    )
  }

  test("main source") {
    val mainPackageName = "com.example.service"
    val mainClassName   = "SomeMain"

    val sourceDoc = SourceGenerator.mainSource(
      mainPackageName,
      mainClassName
    )
    assertEquals(
      sourceDoc.layout,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.AkkaServerless;
        |import org.slf4j.Logger;
        |import org.slf4j.LoggerFactory;
        |
        |import static com.example.service.SomeMainComponentRegistrations.withGeneratedComponentsAdded;
        |
        |public final class SomeMain {
        |    
        |    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
        |    
        |    public static void main(String[] args) throws Exception {
        |        LOG.info("starting the Akka Serverless service");
        |        withGeneratedComponentsAdded(new AkkaServerless())
        |                .start().toCompletableFuture().get();
        |    }
        |}""".stripMargin
    )
  }

}
