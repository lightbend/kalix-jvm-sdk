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

import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

import _root_.java.nio.file.Files
import _root_.java.nio.file.Path
import scala.util.Try

class SourceGeneratorSuite extends munit.FunSuite {
  val log = LoggerFactory.getLogger(getClass)
  implicit val codegenLog = new Log {
    override def debug(message: String): Unit = log.debug(message)
    override def info(message: String): Unit = log.info(message)
    override def warning(message: String): Unit = log.warn(message)
    override def error(message: String): Unit = log.error(message)
  }

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
              sourceDirectory.resolve("com/example/service/persistence/MyEntity1.java")
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
                "com/example/service/persistence/AbstractMyEntity1.java"
              )
            val implSourceFile1 = implSource1.toFile
            val implSource2 =
              generatedSourceDirectory.resolve(
                "com/example/service/persistence/AbstractMyEntity2.java"
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
            val service5Proto = TestData.serviceProto("5")

            val services = Map(
              "com.example.Service1" -> TestData.simpleEntityService(service1Proto, "1"),
              "com.example.Service2" -> TestData.simpleEntityService(service2Proto, "2"),
              "com.example.Service3" -> TestData.simpleEntityService(service3Proto, "3"),
              "com.example.Service4" -> TestData.simpleViewService(service4Proto, "4"),
              "com.example.Service5" -> TestData.simpleActionService(service5Proto)
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
              sources.toSet,
              Set(
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/AbstractMyEntity1.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/MyEntity1Handler.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/MyEntity1Provider.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/MyEntity1TestKit.java"
                ),
                sourceDirectory.resolve("com/example/service/persistence/MyValueEntity2.java"),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/AbstractMyValueEntity2.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/MyValueEntity2Provider.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/MyValueEntity2Handler.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/MyValueEntity2TestKit.java"
                ),
                sourceDirectory.resolve("com/example/service/persistence/MyEntity3.java"),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/AbstractMyEntity3.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/MyEntity3Handler.java"
                ),
                integrationTestSourceDirectory.resolve(
                  "com/example/service/persistence/MyEntity3IntegrationTest.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/MyEntity3TestKit.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/persistence/MyEntity3Provider.java"
                ),
                sourceDirectory.resolve("com/example/service/MyService4View.java"),
                generatedSourceDirectory.resolve(
                  "com/example/service/AbstractMyService4View.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/MyService4ViewHandler.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/MyService4ViewProvider.java"
                ),
                sourceDirectory.resolve("com/example/service/MyService5Action.java"),
                generatedSourceDirectory.resolve(
                  "com/example/service/AbstractMyService5Action.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/MyService5ActionProvider.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/MyService5ActionHandler.java"
                ),
                generatedSourceDirectory.resolve(
                  "com/example/service/AkkaServerlessFactory.java"
                ),
                sourceDirectory.resolve("com/example/service/Main.java")
              )
            )

            // Test that the files were written to
            sources.foreach { (source: Path) =>
              assert(Files.exists(source))
              try {
                assertEquals(Files.readAllBytes(source).head.toChar, '/', s"$source did not start with '/'")
              } catch {
                case e: Throwable => fail(s"Failed to read [$source]: ${e.getMessage}")
              }
            }
          } finally FileUtils.deleteDirectory(generatedSourceDirectory.toFile)
        } finally FileUtils.deleteDirectory(integrationTestSourceDirectory.toFile)
      } finally FileUtils.deleteDirectory(testSourceDirectory.toFile)
    } finally FileUtils.deleteDirectory(sourceDirectory.toFile)
  }

  test("generated component registration source") {
    val service1Proto = TestData.serviceProto("1")
    val service2Proto = TestData.serviceProto("2")
    val service3Proto = TestData.serviceProto("3").copy(pkg = "com.example.service.something")
    val service4Proto = TestData.serviceProto("4").copy(pkg = "com.example.service.view")
    val service5Proto = TestData.serviceProto("5")

    val services = Map(
      "com.example.Service1" -> TestData.simpleEntityService(service1Proto, "1"),
      "com.example.Service2" -> TestData.simpleEntityService(service2Proto, "2"),
      "com.example.Service3" -> TestData.simpleEntityService(service3Proto, "3"),
      "com.example.Service4" -> TestData.simpleViewService(service4Proto, "4"),
      "com.example.Service5" -> TestData.simpleActionService(service5Proto)
    )

    val entities = Map(
      "com.example.Entity1" -> TestData.eventSourcedEntity(suffix = "1"),
      "com.example.Entity2" -> TestData.valueEntity(suffix = "2"),
      "com.example.Entity3" -> TestData.eventSourcedEntity(suffix = "3")
    )

    val mainPackageName = "com.example.service"
    val mainClassName = "SomeMain"

    val generatedSrc = SourceGenerator.akkaServerlessFactorySource(
      mainPackageName,
      ModelBuilder.Model(services, entities)
    )
    assertEquals(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |package com.example.service;
        |
        |import com.akkaserverless.javasdk.AkkaServerless;
        |import com.akkaserverless.javasdk.action.ActionCreationContext;
        |import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityContext;
        |import com.akkaserverless.javasdk.valueentity.ValueEntityContext;
        |import com.akkaserverless.javasdk.view.ViewCreationContext;
        |import com.example.service.persistence.EntityOuterClass1;
        |import com.example.service.persistence.EntityOuterClass2;
        |import com.example.service.persistence.EntityOuterClass3;
        |import com.example.service.persistence.EntityOuterClass4;
        |import com.example.service.persistence.MyEntity1;
        |import com.example.service.persistence.MyEntity1Provider;
        |import com.example.service.persistence.MyEntity3;
        |import com.example.service.persistence.MyEntity3Provider;
        |import com.example.service.persistence.MyValueEntity2;
        |import com.example.service.persistence.MyValueEntity2Provider;
        |import com.example.service.something.ServiceOuterClass3;
        |import com.example.service.view.MyService4View;
        |import com.example.service.view.MyService4ViewProvider;
        |import com.example.service.view.ServiceOuterClass4;
        |import com.external.ExternalDomain;
        |import java.util.function.Function;
        |
        |public final class AkkaServerlessFactory {
        |
        |  public static AkkaServerless withComponents(
        |      Function<EventSourcedEntityContext, MyEntity1> createMyEntity1,
        |      Function<ValueEntityContext, MyValueEntity2> createMyValueEntity2,
        |      Function<EventSourcedEntityContext, MyEntity3> createMyEntity3,
        |      Function<ViewCreationContext, MyService4View> createMyService4View,
        |      Function<ActionCreationContext, MyService5Action> createMyService5Action) {
        |    AkkaServerless akkaServerless = new AkkaServerless();
        |    return akkaServerless
        |      .register(MyEntity1Provider.of(createMyEntity1))
        |      .register(MyEntity3Provider.of(createMyEntity3))
        |      .register(MyService4ViewProvider.of(createMyService4View))
        |      .register(MyService5ActionProvider.of(createMyService5Action))
        |      .register(MyValueEntity2Provider.of(createMyValueEntity2));
        |  }
        |}""".stripMargin
    )
  }

  test("generated component registration source for a view without update handlers") {
    val serviceProto = TestData.serviceProto().copy(pkg = "com.example.service.view")

    val services = Map(
      "com.example.Service" -> TestData.simpleViewService(serviceProto).copy(transformedUpdates = Nil)
    )

    val entities = Map.empty[String, ModelBuilder.Entity]

    val mainPackageName = "com.example.service"
    val mainClassName = "SomeMain"

    val generatedSrc = SourceGenerator.akkaServerlessFactorySource(
      mainPackageName,
      ModelBuilder.Model(services, entities)
    )
    assertEquals(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |package com.example.service;
        |
        |import com.akkaserverless.javasdk.AkkaServerless;
        |import com.akkaserverless.javasdk.view.ViewCreationContext;
        |import com.example.service.persistence.EntityOuterClass;
        |import com.example.service.view.MyServiceView;
        |import com.example.service.view.MyServiceViewProvider;
        |import com.example.service.view.ServiceOuterClass;
        |import java.util.function.Function;
        |
        |public final class AkkaServerlessFactory {
        |
        |  public static AkkaServerless withComponents(
        |      Function<ViewCreationContext, MyServiceView> createMyServiceView) {
        |    AkkaServerless akkaServerless = new AkkaServerless();
        |    return akkaServerless
        |      .register(MyServiceViewProvider.of(createMyServiceView));
        |  }
        |}""".stripMargin
    )
  }

  test("main source") {
    val mainPackageName = "com.example.service"
    val mainClassName = "SomeMain"

    val entities = Map(
      "com.example.Entity1" -> TestData.eventSourcedEntity(suffix = "1"),
      "com.example.Entity2" -> TestData.valueEntity(suffix = "2"),
      "com.example.Entity3" -> TestData.eventSourcedEntity(suffix = "3")
    )

    val services = Map(
      "com.example.Service1" -> TestData.simpleActionService()
    )

    val generatedSrc = SourceGenerator.mainSource(
      mainPackageName,
      mainClassName,
      entities,
      services
    )
    assertEquals(
      generatedSrc,
      """|/* This code was generated by Akka Serverless tooling.
         | * As long as this file exists it will not be re-generated.
         | * You are free to make changes to this file.
         | */
         |
         |package com.example.service;
         |
         |import com.akkaserverless.javasdk.AkkaServerless;
         |import org.slf4j.Logger;
         |import org.slf4j.LoggerFactory;
         |import com.example.service.MyServiceAction;
         |import com.example.service.persistence.MyEntity1;
         |import com.example.service.persistence.MyEntity3;
         |import com.example.service.persistence.MyValueEntity2;
         |
         |public final class SomeMain {
         |
         |  private static final Logger LOG = LoggerFactory.getLogger(SomeMain.class);
         |
         |  public static AkkaServerless createAkkaServerless() {
         |    // The AkkaServerlessFactory automatically registers any generated Actions, Views or Entities,
         |    // and is kept up-to-date with any changes in your protobuf definitions.
         |    // If you prefer, you may remove this and manually register these components in a
         |    // `new AkkaServerless()` instance.
         |    return AkkaServerlessFactory.withComponents(
         |      MyEntity1::new,
         |      MyValueEntity2::new,
         |      MyEntity3::new,
         |      MyServiceAction::new);
         |  }
         |
         |  public static void main(String[] args) throws Exception {
         |    LOG.info("starting the Akka Serverless service");
         |    createAkkaServerless().start();
         |  }
         |}""".stripMargin
    )
  }

}
