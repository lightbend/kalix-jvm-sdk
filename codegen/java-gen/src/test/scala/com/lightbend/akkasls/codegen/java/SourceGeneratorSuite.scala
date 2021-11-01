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

class SourceGeneratorSuite extends munit.FunSuite {
  val log = LoggerFactory.getLogger(getClass)
  implicit val codegenLog = new Log {
    override def debug(message: String): Unit = log.debug(message)
    override def info(message: String): Unit = log.info(message)
  }

  private val testData = TestData.javaStyle

  def domainType(name: String): ModelBuilder.TypeArgument =
    ModelBuilder.TypeArgument(
      name,
      testData.domainProto(),
      TestData.guessDescriptor(testData.domainProto().name, testData.domainProto()))

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
          val generatedTestSourceDirectory =
            Files.createTempDirectory("generated-test-source-generator-test")
          try {

            val source1 =
              sourceDirectory.resolve("com/example/service/domain/MyEntity1.java")
            val sourceFile1 = source1.toFile
            FileUtils.forceMkdir(sourceFile1.getParentFile)
            FileUtils.touch(sourceFile1)

            val testSource2 =
              testSourceDirectory.resolve("com/example/service/domain/MyValueEntity2Test.java")
            val testSourceFile2 = testSource2.toFile
            FileUtils.forceMkdir(testSourceFile2.getParentFile)
            FileUtils.touch(testSourceFile2)

            val integrationTestSource2 =
              integrationTestSourceDirectory.resolve("com/example/service/domain/MyValueEntity2IntegrationTest.java")
            val integrationTestSourceFile2 = integrationTestSource2.toFile
            FileUtils.forceMkdir(integrationTestSourceFile2.getParentFile)
            FileUtils.touch(integrationTestSourceFile2)

            val implSource1 =
              generatedSourceDirectory.resolve("com/example/service/domain/AbstractMyEntity1.java")
            val implSourceFile1 = implSource1.toFile
            val implSource2 =
              generatedSourceDirectory.resolve("com/example/service/domain/AbstractMyEntity2.java")
            val implSourceFile2 = implSource2.toFile
            FileUtils.forceMkdir(implSourceFile1.getParentFile)
            FileUtils.touch(implSourceFile1)
            FileUtils.touch(implSourceFile2)

            val service1Proto = testData.serviceProto("1")
            val service2Proto = testData.serviceProto("2")
            val service3Proto =
              testData.serviceProto("3").copy(protoPackage = "com.example.service.something")
            val service4Proto = testData.serviceProto("4")
            val service5Proto = testData.serviceProto("5")
            val service6Proto = testData.serviceProto("6")

            val services = Map(
              "com.example.Service1" -> testData.simpleEntityService(service1Proto, "1"),
              "com.example.Service2" -> testData.simpleEntityService(service2Proto, "2"),
              "com.example.Service3" -> testData.simpleEntityService(service3Proto, "3"),
              "com.example.Service4" -> testData.simpleViewService(service4Proto, "4"),
              "com.example.Service5" -> testData.simpleActionService(service5Proto),
              "com.example.Service6" -> testData.simpleEntityService(service6Proto, "6"))

            val entities = Map(
              "com.example.Entity1" -> testData.eventSourcedEntity(suffix = "1"),
              "com.example.Entity2" -> testData.valueEntity(suffix = "2"),
              "com.example.Entity3" -> testData.eventSourcedEntity(suffix = "3"),
              "com.example.Entity6" -> testData.replicatedEntity(
                ModelBuilder.ReplicatedSet(domainType("SomeElement")),
                suffix = "6"))

            val sources = SourceGenerator.generate(
              ModelBuilder.Model(services, entities),
              sourceDirectory,
              testSourceDirectory,
              integrationTestSourceDirectory,
              generatedSourceDirectory,
              generatedTestSourceDirectory,
              "com.example.service.Main")

            assertEquals(Files.size(source1), 0L)
            assertEquals(Files.size(testSource2), 0L)

            assertEquals(
              sources.toSet,
              Set(
                generatedSourceDirectory.resolve("com/example/service/domain/AbstractMyEntity1.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/MyEntity1Router.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/MyEntity1Provider.java"),
                generatedTestSourceDirectory.resolve("com/example/service/domain/MyEntity1TestKit.java"),
                testSourceDirectory.resolve("com/example/service/domain/MyEntity1Test.java"),
                integrationTestSourceDirectory.resolve("com/example/service/domain/MyEntity1IntegrationTest.java"),
                sourceDirectory.resolve("com/example/service/domain/MyEntity1.java"),
                sourceDirectory.resolve("com/example/service/domain/MyValueEntity2.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/AbstractMyValueEntity2.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/MyValueEntity2Provider.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/MyValueEntity2Router.java"),
                generatedTestSourceDirectory.resolve("com/example/service/domain/MyValueEntity2TestKit.java"),
                integrationTestSourceDirectory.resolve("com/example/service/domain/MyValueEntity2IntegrationTest.java"),
                sourceDirectory.resolve("com/example/service/domain/MyEntity3.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/AbstractMyEntity3.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/MyEntity3Router.java"),
                integrationTestSourceDirectory.resolve("com/example/service/domain/MyEntity3IntegrationTest.java"),
                testSourceDirectory.resolve("com/example/service/domain/MyEntity3Test.java"),
                generatedTestSourceDirectory.resolve("com/example/service/domain/MyEntity3TestKit.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/MyEntity3Provider.java"),
                sourceDirectory.resolve("com/example/service/MyService4ViewImpl.java"),
                generatedSourceDirectory.resolve("com/example/service/AbstractMyService4View.java"),
                generatedSourceDirectory.resolve("com/example/service/MyService4ViewRouter.java"),
                generatedSourceDirectory.resolve("com/example/service/MyService4ViewProvider.java"),
                sourceDirectory.resolve("com/example/service/MyService5Action.java"),
                testSourceDirectory.resolve("com/example/service/MyService5ActionTest.java"),
                generatedSourceDirectory.resolve("com/example/service/AbstractMyService5Action.java"),
                generatedSourceDirectory.resolve("com/example/service/MyService5ActionProvider.java"),
                generatedSourceDirectory.resolve("com/example/service/MyService5ActionRouter.java"),
                generatedTestSourceDirectory.resolve("com/example/service/MyService5ActionTestKit.java"),
                sourceDirectory.resolve("com/example/service/domain/MyReplicatedEntity6.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/AbstractMyReplicatedEntity6.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/MyReplicatedEntity6Router.java"),
                generatedSourceDirectory.resolve("com/example/service/domain/MyReplicatedEntity6Provider.java"),
                /* generatedTestSourceDirectory.resolve(
                  "com/example/service/domain/MyReplicatedEntity6TestKit.java"
                ), */
                integrationTestSourceDirectory.resolve(
                  "com/example/service/domain/MyReplicatedEntity6IntegrationTest.java"),
                generatedSourceDirectory.resolve("com/example/service/AkkaServerlessFactory.java"),
                generatedSourceDirectory.resolve("com/example/service/ComponentsImpl.java"),
                generatedSourceDirectory.resolve("com/example/service/Components.java"),
                sourceDirectory.resolve("com/example/service/Main.java")))

            // Test that the files were written to
            sources.foreach { (source: Path) =>
              assert(Files.exists(source))
              if (Files.size(source) > 0) // otherwise we assume it has been touched above
                try {
                  val firstCharacter = Files.readAllBytes(source).head.toChar
                  if (firstCharacter != '/' && firstCharacter != 'p')
                    fail(s"$source did not start with 'p' (for 'package') or '/' (for comment)")
                } catch {
                  case e: Throwable => fail(s"Failed to read [$source]: ${e.getMessage}")
                }
            }
          } finally FileUtils.deleteDirectory(generatedSourceDirectory.toFile)
        } finally FileUtils.deleteDirectory(integrationTestSourceDirectory.toFile)
      } finally FileUtils.deleteDirectory(testSourceDirectory.toFile)
    } finally FileUtils.deleteDirectory(sourceDirectory.toFile)
  }

}
