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

  val log = LoggerFactory.getLogger(getClass)

  val model = generateModel
  val service: ModelBuilder.ActionService = {
    model.get.services("com.example.actions.CounterJournalToTopic").asInstanceOf[ModelBuilder.ActionService]
  }
  //This prints the contents of the model, services or entity. It's necessary to
  // see how the user-function.desc (see generateModel below) maps to ModelBuilder objects.
  // println(munit.Assertions.munitPrint(service.commands))

  test(
    "it can generate an specific TestKit for the user-function file " +
    "in test/resources/descriptor-sets build from the proto files" +
    "from samples/java-eventsourced-counter") {

    val sourceCode = ActionTestKitGenerator.generateSourceCode(service)

    val expected =
      """package com.example.actions;
        |
        |import com.akkaserverless.javasdk.action.Action.Effect;
        |import com.akkaserverless.javasdk.action.ActionCreationContext;
        |import com.akkaserverless.javasdk.impl.action.ActionEffectImpl;
        |import com.akkaserverless.javasdk.testkit.ActionResult;
        |import com.akkaserverless.javasdk.testkit.impl.ActionResultImpl;
        |import com.akkaserverless.javasdk.testkit.impl.TestKitActionContext;
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
        |// This code is managed by Akka Serverless tooling.
        |// It will be re-generated to reflect any changes to your protobuf definitions.
        |// DO NOT EDIT
        |
        |public final class CounterJournalToTopicActionTestKit {
        |
        |  private Function<ActionCreationContext, CounterJournalToTopicAction> actionFactory;
        |
        |  private CounterJournalToTopicAction createAction() {
        |    CounterJournalToTopicAction action = actionFactory.apply(new TestKitActionContext());
        |    action._internalSetActionContext(Optional.of(new TestKitActionContext()));
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
        |  private <E> ActionResult<E> interpretEffects(Effect<E> effect) {
        |    return new ActionResultImpl(effect);
        |  }
        |
        |  public ActionResult<CounterTopicApi.Increased> increase(CounterDomain.ValueIncreased valueIncreased) {
        |    Effect<CounterTopicApi.Increased> effect = createAction().increase(valueIncreased);
        |    return interpretEffects(effect);
        |  }
        |
        |  public ActionResult<CounterTopicApi.Decreased> decrease(CounterDomain.ValueDecreased valueDecreased) {
        |    Effect<CounterTopicApi.Decreased> effect = createAction().decrease(valueDecreased);
        |    return interpretEffects(effect);
        |  }
        |
        |  public ActionResult<Empty> ignore(Any any) {
        |    Effect<Empty> effect = createAction().ignore(any);
        |    return interpretEffects(effect);
        |  }
        |
        |}""".stripMargin
    assertNoDiff(sourceCode, expected)
  }

  test("it can generate an specific Test stub for the entity") {

    val sourceCode = ActionTestKitGenerator.generateTestSourceCode(service)

    val expected =
      """package com.example.actions;
        |
        |import com.akkaserverless.javasdk.testkit.ActionResult;
        |import com.example.actions.CounterJournalToTopicAction;
        |import com.example.actions.CounterJournalToTopicActionTestKit;
        |import com.example.actions.CounterTopicApi;
        |import com.example.domain.CounterDomain;
        |import com.google.protobuf.Any;
        |import com.google.protobuf.Empty;
        |import org.junit.Test;
        |import static org.junit.Assert.*;
        |
        |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
        |//
        |// As long as this file exists it will not be overwritten: you can maintain it yourself,
        |// or delete it so it is regenerated as needed.
        |
        |public class CounterJournalToTopicActionTest {
        |
        |  @Test
        |  public void exampleTest() {
        |    CounterJournalToTopicActionTestKit testKit = CounterJournalToTopicActionTestKit.of(CounterJournalToTopicAction::new);
        |    // use the testkit to execute a command
        |    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
        |    // verify the response
        |    // SomeResponse actualResponse = result.getReply();
        |    // assertEquals(expectedResponse, actualResponse);
        |  }
        |
        |  @Test
        |  public void increaseTest() {
        |    CounterJournalToTopicActionTestKit testKit = CounterJournalToTopicActionTestKit.of(CounterJournalToTopicAction::new);
        |    // ActionResult<CounterTopicApi.Increased> result = testKit.increase(CounterDomain.ValueIncreased.newBuilder()...build());
        |  }
        |
        |  @Test
        |  public void decreaseTest() {
        |    CounterJournalToTopicActionTestKit testKit = CounterJournalToTopicActionTestKit.of(CounterJournalToTopicAction::new);
        |    // ActionResult<CounterTopicApi.Decreased> result = testKit.decrease(CounterDomain.ValueDecreased.newBuilder()...build());
        |  }
        |
        |  @Test
        |  public void ignoreTest() {
        |    CounterJournalToTopicActionTestKit testKit = CounterJournalToTopicActionTestKit.of(CounterJournalToTopicAction::new);
        |    // ActionResult<Empty> result = testKit.ignore(Any.newBuilder()...build());
        |  }
        |
        |}
        |""".stripMargin

    assertNoDiff(sourceCode, expected)
  }

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
