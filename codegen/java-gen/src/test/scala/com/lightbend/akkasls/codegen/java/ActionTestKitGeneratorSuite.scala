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

import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors

import _root_.java.io.FileInputStream
import _root_.java.nio.file.Paths
import scala.jdk.CollectionConverters._
import scala.util.Using
import com.google.protobuf.ExtensionRegistry
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.util.Try

class ActionTestKitGeneratorSuite extends munit.FunSuite {
  private val testData = TestData.javaStyle

  test(
    "it can generate an specific TestKit for the proto files " +
    "in test/resources/testkit") {

    val packageName = "com.example.actions"
    val model = generateModel
    val className = "CounterJournalToTopicAction"
    val service: ModelBuilder.ActionService = {
      model.get.services("com.example.actions.CounterJournalToTopic").asInstanceOf[ModelBuilder.ActionService]

    }
    val entity: ModelBuilder.EventSourcedEntity = {
      model.get.entities("com.example.domain.Counter").asInstanceOf[ModelBuilder.EventSourcedEntity]
    }

    //This prints the contents of the model, services or entity. Necessary to
    // see how the user-function.desc (see generateModel below) maps to ModelBuilder objects.
    // println(munit.Assertions.munitPrint(entity))

    val sourceCode = ActionTestKitGenerator.generateSourceCode(entity, service, packageName, className)

    val expected =
      """|/* This code is managed by Akka Serverless tooling.
          | * It will be re-generated to reflect any changes to your protobuf definitions.
          | * DO NOT EDIT
          | */
          |package com.example.actions;
          |
          |import com.akkaserverless.javasdk.action.Action;
          |import com.akkaserverless.javasdk.action.ActionCreationContext;
          |import com.akkaserverless.javasdk.impl.action.ActionEffectImpl;
          |import com.akkaserverless.javasdk.testkit.impl.ActionResultImpl;
          |import com.akkaserverless.javasdk.testkit.impl.StubActionContext;
          |import com.akkaserverless.javasdk.testkit.impl.StubActionCreationContext;
          |import com.example.actions.CounterJournalToTopicAction;
          |import com.example.actions.CounterTopicApi;
          |import com.example.domain.CounterDomain;
          |import com.google.protobuf.Any;
          |import com.google.protobuf.Empty;
          |import java.util.ArrayList;
          |import java.util.List;
          |import java.util.Optional;
          |import java.util.function.Function;
          |
          |public final class CounterJournalToTopicActionTestKit {
          |
          |  private Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory;
          |
          |  private CounterJournalToTopicAction createAction() {
          |    CounterJournalToTopicAction action = actionFactory.apply(new StubActionCreationContext());
          |    action._internalSetActionContext(Optional.of(new StubActionContext()));
          |    return action;
          |  };
          |
          |  public static CounterJournalToTopicActionTestKit of(Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory) {
          |    return new CounterJournalToTopicActionTestKit(actionFactory);
          |  }
          |
          |  private CounterJournalToTopicActionTestKit(Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory) {
          |    this.actionFactory = actionFactory;
          |  }
          |
          |  private <E> ActionResult<E> interpretEffects(Action.Effect<E> effect) {
          |    return new ActionResultImpl(effect);
          |  }
          |
          |  public ActionResult<CounterTopicApi.Increased> increase(CounterDomain.ValueIncreased event) {
          |    Action.Effect<CounterTopicApi.Increased> effect = createAction().increase(event);
          |    return interpretEffects(effect);
          |  }
          |
          |  public ActionResult<CounterTopicApi.Decreased> decrease(CounterDomain.ValueDecreased event) {
          |    Action.Effect<CounterTopicApi.Decreased> effect = createAction().decrease(event);
          |    return interpretEffects(effect);
          |  }
          |
          |  public ActionResult<Empty> ignore(CounterDomain.Any event) {
          |    Action.Effect<Empty> effect = createAction().ignore(event);
          |    return interpretEffects(effect);
          |  }
          |
          |}""".stripMargin

    assertNoDiff(sourceCode, expected)
  }

  test("it can generate an specific Test stub for the entity") {}

  /**
   * This ModelBuilder.EventSourcedEntity is equivalent to the entity in
   * test/resources/testkit/shoppingcart_domain.proto
   */
  def generateModel(): Try[ModelBuilder.Model] = {

    val log = LoggerFactory.getLogger(getClass)
    implicit val codegenLog = new Log {
      override def debug(message: String): Unit = log.debug(message)
      override def info(message: String): Unit = log.info(message)
    }
    implicit val e = TestFullyQualifiedNameExtractor

    val testFilesPath = Paths.get(getClass.getClassLoader.getResource("descriptor-sets").toURI)
    val descriptorFilePath =
      testFilesPath.resolve("java-eventsourced-counter-user-function.desc")

    val registry = ExtensionRegistry.newInstance()
    registry.add(com.akkaserverless.Annotations.service)
    registry.add(com.akkaserverless.Annotations.file)

    Using(new FileInputStream(descriptorFilePath.toFile)) { fis =>
      val fileDescSet = FileDescriptorSet.parseFrom(fis, registry)
      val fileList = fileDescSet.getFileList.asScala

      val descriptors: mutable.Seq[Descriptors.FileDescriptor] =
        fileList.foldLeft(Array[Descriptors.FileDescriptor]())((acc, file) => accumulatedBuildFrom(acc, file))

      val model = ModelBuilder.introspectProtobufClasses(descriptors)
      model
    }

  }

  private def accumulatedBuildFrom(
      fileDescriptors: Array[Descriptors.FileDescriptor],
      file: DescriptorProtos.FileDescriptorProto): Array[Descriptors.FileDescriptor] = {
    fileDescriptors ++ List(Descriptors.FileDescriptor.buildFrom(file, fileDescriptors.toArray, true))
  }

}
