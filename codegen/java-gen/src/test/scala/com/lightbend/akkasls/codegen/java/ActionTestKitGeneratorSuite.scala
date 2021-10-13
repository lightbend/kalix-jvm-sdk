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
  val service: ModelBuilder.ActionService = testData.simpleActionService()

  test(
    "it can generate an specific TestKit for the user-function file " +
    "in test/resources/descriptor-sets build from the proto files" +
    "from samples/java-eventsourced-counter") {

    val sourceCode = ActionTestKitGenerator.generateSourceCode(service)

    val expected =
      """package com.example.service;
        |
        |import akka.NotUsed;
        |import akka.stream.javadsl.Source;
        |import com.akkaserverless.javasdk.action.Action.Effect;
        |import com.akkaserverless.javasdk.action.ActionCreationContext;
        |import com.akkaserverless.javasdk.impl.action.ActionEffectImpl;
        |import com.akkaserverless.javasdk.testkit.ActionResult;
        |import com.akkaserverless.javasdk.testkit.impl.ActionResultImpl;
        |import com.akkaserverless.javasdk.testkit.impl.TestKitActionContext;
        |import com.example.service.MyServiceAction;
        |import com.example.service.ServiceOuterClass;
        |import com.external.Empty;
        |import java.util.ArrayList;
        |import java.util.List;
        |import java.util.Optional;
        |import java.util.function.Function;
        |
        |// This code is managed by Akka Serverless tooling.
        |// It will be re-generated to reflect any changes to your protobuf definitions.
        |// DO NOT EDIT
        |
        |public final class MyServiceActionTestKit {
        |
        |  private Function<ActionCreationContext, MyServiceAction> actionFactory;
        |
        |  private MyServiceAction createAction() {
        |    MyServiceAction action = actionFactory.apply(new TestKitActionContext());
        |    action._internalSetActionContext(Optional.of(new TestKitActionContext()));
        |    return action;
        |  };
        |
        |  public static MyServiceActionTestKit of(Function<ActionCreationContext, MyServiceAction> actionFactory) {
        |    return new MyServiceActionTestKit(actionFactory);
        |  }
        |
        |  private MyServiceActionTestKit(Function<ActionCreationContext, MyServiceAction> actionFactory) {
        |    this.actionFactory = actionFactory;
        |  }
        |
        |  private <E> ActionResult<E> interpretEffects(Effect<E> effect) {
        |    return new ActionResultImpl(effect);
        |  }
        |
        |  public ActionResult<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest) {
        |    Effect<Empty> effect = createAction().simpleMethod(myRequest);
        |    return interpretEffects(effect);
        |  }
        |
        |  public Source<ActionResult<Empty>, akka.NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest) {
        |    Source<Effect<Empty>, akka.NotUsed> effect = createAction().streamedOutputMethod(myRequest);
        |    return effect.map(e -> interpretEffects(e));
        |  }
        |
        |  public ActionResult<Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest) {
        |    Effect<Empty> effect = createAction().streamedInputMethod(myRequest);
        |    return interpretEffects(effect);
        |  }
        |
        |  public Source<ActionResult<Empty>, akka.NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, akka.NotUsed> myRequest) {
        |    Source<Effect<Empty>, akka.NotUsed> effect = createAction().fullStreamedMethod(myRequest);
        |    return effect.map(e -> interpretEffects(e));
        |  }
        |
        |}""".stripMargin
    assertNoDiff(sourceCode, expected)
  }

  test("it can generate an specific Test stub for the entity") {

    val sourceCode = ActionTestKitGenerator.generateTestSourceCode(service)
    println(sourceCode)

    val expected =
      """package com.example.service;
        |
        |import akka.NotUsed;
        |import akka.stream.javadsl.Source;
        |import com.akkaserverless.javasdk.testkit.ActionResult;
        |import com.example.service.MyServiceAction;
        |import com.example.service.MyServiceActionTestKit;
        |import com.example.service.ServiceOuterClass;
        |import com.external.Empty;
        |import org.junit.Test;
        |import static org.junit.Assert.*;
        |
        |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
        |//
        |// As long as this file exists it will not be overwritten: you can maintain it yourself,
        |// or delete it so it is regenerated as needed.
        |
        |public class MyServiceActionTest {
        |
        |  @Test
        |  public void exampleTest() {
        |    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
        |    // use the testkit to execute a command
        |    // ActionResult<SomeResponse> result = testKit.someOperation(SomeRequest);
        |    // verify the response
        |    // SomeResponse actualResponse = result.getReply();
        |    // assertEquals(expectedResponse, actualResponse);
        |  }
        |
        |  @Test
        |  public void simpleMethodTest() {
        |    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
        |    // ActionResult<Empty> result = testKit.simpleMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
        |  }
        |
        |  @Test
        |  public void streamedOutputMethodTest() {
        |    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
        |    // Source<ActionResult<Empty>, akka.NotUsed> result = testKit.streamedOutputMethod(ServiceOuterClass.MyRequest.newBuilder()...build());
        |  }
        |
        |  @Test
        |  public void streamedInputMethodTest() {
        |    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
        |    // ActionResult<Empty> result = testKit.streamedInputMethod(Source.single(ServiceOuterClass.MyRequest.newBuilder()...build()));
        |  }
        |
        |  @Test
        |  public void fullStreamedMethodTest() {
        |    MyServiceActionTestKit testKit = MyServiceActionTestKit.of(MyServiceAction::new);
        |    // Source<ActionResult<Empty>, akka.NotUsed> result = testKit.fullStreamedMethod(Source.single(ServiceOuterClass.MyRequest.newBuilder()...build()));
        |  }
        |
        |}""".stripMargin

    assertNoDiff(sourceCode, expected)
  }

}
