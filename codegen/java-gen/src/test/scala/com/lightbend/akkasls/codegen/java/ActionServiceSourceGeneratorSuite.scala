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

class ActionServiceSourceGeneratorSuite extends munit.FunSuite {
  private val testData = TestData.javaStyle

  test("Action source generation") {

    val service = testData.simpleActionService()

    val generatedSrc =
      ActionServiceSourceGenerator.actionSource(service)
    assertNoDiff(
      generatedSrc,
      """package com.example.service;
        |
        |import akka.NotUsed;
        |import akka.stream.javadsl.Source;
        |import com.akkaserverless.javasdk.action.ActionCreationContext;
        |import com.external.Empty;
        |
        |/**
        | * An action.
        | *
        | * This class was initially generated based on the .proto definition by Akka Serverless tooling.
        | *
        | * As long as this file exists it will not be overwritten: you can maintain it yourself,
        | * or delete it so it is regenerated as needed.
        | */
        |public class MyServiceAction extends AbstractMyServiceAction {
        |
        |  public MyServiceAction(ActionCreationContext creationContext) {}
        |
        |  /** Handler for "SimpleMethod". */
        |  @Override
        |  public Effect<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest) {
        |    throw new RuntimeException("The command handler for `SimpleMethod` is not implemented, yet");
        |  }
        |
        |  /** Handler for "StreamedOutputMethod". */
        |  @Override
        |  public Source<Effect<Empty>, NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest) {
        |    throw new RuntimeException("The command handler for `StreamedOutputMethod` is not implemented, yet");
        |  }
        |
        |  /** Handler for "StreamedInputMethod". */
        |  @Override
        |  public Effect<Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc) {
        |    throw new RuntimeException("The command handler for `StreamedInputMethod` is not implemented, yet");
        |  }
        |
        |  /** Handler for "FullStreamedMethod". */
        |  @Override
        |  public Source<Effect<Empty>, NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc) {
        |    throw new RuntimeException("The command handler for `FullStreamedMethod` is not implemented, yet");
        |  }
        |}
        |""".stripMargin)
  }

  test("Action source generation with 'Action' in the name") {

    val packageNaming = testData.serviceProto().copy(name = "MyServiceAction")
    val service = testData.simpleActionService(packageNaming)

    val generatedSrc =
      ActionServiceSourceGenerator.actionSource(service)
    assertNoDiff(
      generatedSrc,
      """package com.example.service;
        |
        |import akka.NotUsed;
        |import akka.stream.javadsl.Source;
        |import com.akkaserverless.javasdk.action.ActionCreationContext;
        |import com.external.Empty;
        |
        |/**
        | * An action.
        | *
        | * This class was initially generated based on the .proto definition by Akka Serverless tooling.
        | *
        | * As long as this file exists it will not be overwritten: you can maintain it yourself,
        | * or delete it so it is regenerated as needed.
        | */
        |public class MyServiceActionImpl extends AbstractMyServiceAction {
        |
        |  public MyServiceActionImpl(ActionCreationContext creationContext) {}
        |
        |  /** Handler for "SimpleMethod". */
        |  @Override
        |  public Effect<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest) {
        |    throw new RuntimeException("The command handler for `SimpleMethod` is not implemented, yet");
        |  }
        |
        |  /** Handler for "StreamedOutputMethod". */
        |  @Override
        |  public Source<Effect<Empty>, NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest) {
        |    throw new RuntimeException("The command handler for `StreamedOutputMethod` is not implemented, yet");
        |  }
        |
        |  /** Handler for "StreamedInputMethod". */
        |  @Override
        |  public Effect<Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc) {
        |    throw new RuntimeException("The command handler for `StreamedInputMethod` is not implemented, yet");
        |  }
        |
        |  /** Handler for "FullStreamedMethod". */
        |  @Override
        |  public Source<Effect<Empty>, NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc) {
        |    throw new RuntimeException("The command handler for `FullStreamedMethod` is not implemented, yet");
        |  }
        |}
        |""".stripMargin)
  }

  test("Action abstract class source generation") {
    val service = testData.simpleActionService()

    val generatedSrc =
      ActionServiceSourceGenerator.abstractActionSource(service)
    assertNoDiff(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |package com.example.service;
        |
        |import akka.NotUsed;
        |import akka.stream.javadsl.Source;
        |import com.akkaserverless.javasdk.action.Action;
        |import com.external.Empty;
        |
        |/** An action. */
        |public abstract class AbstractMyServiceAction extends Action {
        |
        |  /** Handler for "SimpleMethod". */
        |  public abstract Effect<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest);
        |
        |  /** Handler for "StreamedOutputMethod". */
        |  public abstract Source<Effect<Empty>, NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest);
        |
        |  /** Handler for "StreamedInputMethod". */
        |  public abstract Effect<Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc);
        |
        |  /** Handler for "FullStreamedMethod". */
        |  public abstract Source<Effect<Empty>, NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc);
        |}
        |""".stripMargin)
  }

  /** confirms that the generated abstract class doesn't get a 'Impl' in the name */
  test("Action abstract class source generation with 'Action' in the name") {

    val packageNaming = testData.serviceProto().copy(name = "MyServiceAction")
    val service = testData.simpleActionService(packageNaming)

    val generatedSrc =
      ActionServiceSourceGenerator.abstractActionSource(service)
    assertNoDiff(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |package com.example.service;
        |
        |import akka.NotUsed;
        |import akka.stream.javadsl.Source;
        |import com.akkaserverless.javasdk.action.Action;
        |import com.external.Empty;
        |
        |/** An action. */
        |public abstract class AbstractMyServiceAction extends Action {
        |
        |  /** Handler for "SimpleMethod". */
        |  public abstract Effect<Empty> simpleMethod(ServiceOuterClass.MyRequest myRequest);
        |
        |  /** Handler for "StreamedOutputMethod". */
        |  public abstract Source<Effect<Empty>, NotUsed> streamedOutputMethod(ServiceOuterClass.MyRequest myRequest);
        |
        |  /** Handler for "StreamedInputMethod". */
        |  public abstract Effect<Empty> streamedInputMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc);
        |
        |  /** Handler for "FullStreamedMethod". */
        |  public abstract Source<Effect<Empty>, NotUsed> fullStreamedMethod(Source<ServiceOuterClass.MyRequest, NotUsed> myRequestSrc);
        |}
        |""".stripMargin)
  }

  test("Action Handler source generation") {
    val service = testData.simpleActionService()

    val generatedSrc =
      ActionServiceSourceGenerator.actionHandler(service)
    assertNoDiff(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |package com.example.service;
        |
        |import akka.NotUsed;
        |import akka.stream.javadsl.Source;
        |import com.akkaserverless.javasdk.action.Action;
        |import com.akkaserverless.javasdk.action.MessageEnvelope;
        |import com.akkaserverless.javasdk.impl.action.ActionHandler;
        |import com.external.Empty;
        |
        |public class MyServiceActionHandler extends ActionHandler<MyServiceAction> {
        |
        |  public MyServiceActionHandler(MyServiceAction actionBehavior) {
        |    super(actionBehavior);
        |  }
        |
        |  @Override
        |  public Action.Effect<?> handleUnary(String commandName, MessageEnvelope<Object> message) {
        |    switch (commandName) {
        |      case "SimpleMethod":
        |        return action()
        |                 .simpleMethod((ServiceOuterClass.MyRequest) message.payload());
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |
        |  @Override
        |  @SuppressWarnings("unchecked")
        |  public Source<Action.Effect<?>, NotUsed> handleStreamedOut(String commandName, MessageEnvelope<Object> message) {
        |    switch (commandName) {
        |      case "StreamedOutputMethod":
        |        return (Source<Action.Effect<?>, NotUsed>)(Object) action()
        |                 .streamedOutputMethod((ServiceOuterClass.MyRequest) message.payload());
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |
        |  @Override
        |  public Action.Effect<?> handleStreamedIn(String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
        |    switch (commandName) {
        |      case "StreamedInputMethod":
        |        return action()
        |                 .streamedInputMethod(stream.map(el -> (ServiceOuterClass.MyRequest) el.payload()));
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |
        |  @Override
        |  @SuppressWarnings("unchecked")
        |  public Source<Action.Effect<?>, NotUsed> handleStreamed(String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
        |    switch (commandName) {
        |      case "FullStreamedMethod":
        |        return (Source<Action.Effect<?>, NotUsed>)(Object) action()
        |                 .fullStreamedMethod(stream.map(el -> (ServiceOuterClass.MyRequest) el.payload()));
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |}
        |""".stripMargin)
  }

  test("Action Handler source generation 'Action' in the name") {

    val packageNaming = testData.serviceProto().copy(name = "MyServiceAction")
    val service = testData.simpleActionService(packageNaming)

    val generatedSrc =
      ActionServiceSourceGenerator.actionHandler(service)
    assertNoDiff(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |package com.example.service;
        |
        |import akka.NotUsed;
        |import akka.stream.javadsl.Source;
        |import com.akkaserverless.javasdk.action.Action;
        |import com.akkaserverless.javasdk.action.MessageEnvelope;
        |import com.akkaserverless.javasdk.impl.action.ActionHandler;
        |import com.external.Empty;
        |
        |public class MyServiceActionHandler extends ActionHandler<MyServiceActionImpl> {
        |
        |  public MyServiceActionHandler(MyServiceActionImpl actionBehavior) {
        |    super(actionBehavior);
        |  }
        |
        |  @Override
        |  public Action.Effect<?> handleUnary(String commandName, MessageEnvelope<Object> message) {
        |    switch (commandName) {
        |      case "SimpleMethod":
        |        return action()
        |                 .simpleMethod((ServiceOuterClass.MyRequest) message.payload());
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |
        |  @Override
        |  @SuppressWarnings("unchecked")
        |  public Source<Action.Effect<?>, NotUsed> handleStreamedOut(String commandName, MessageEnvelope<Object> message) {
        |    switch (commandName) {
        |      case "StreamedOutputMethod":
        |        return (Source<Action.Effect<?>, NotUsed>)(Object) action()
        |                 .streamedOutputMethod((ServiceOuterClass.MyRequest) message.payload());
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |
        |  @Override
        |  public Action.Effect<?> handleStreamedIn(String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
        |    switch (commandName) {
        |      case "StreamedInputMethod":
        |        return action()
        |                 .streamedInputMethod(stream.map(el -> (ServiceOuterClass.MyRequest) el.payload()));
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |
        |  @Override
        |  @SuppressWarnings("unchecked")
        |  public Source<Action.Effect<?>, NotUsed> handleStreamed(String commandName, Source<MessageEnvelope<Object>, NotUsed> stream) {
        |    switch (commandName) {
        |      case "FullStreamedMethod":
        |        return (Source<Action.Effect<?>, NotUsed>)(Object) action()
        |                 .fullStreamedMethod(stream.map(el -> (ServiceOuterClass.MyRequest) el.payload()));
        |      default:
        |        throw new ActionHandler.HandlerNotFound(commandName);
        |    }
        |  }
        |}
        |""".stripMargin)
  }

  test("Action Provider source generation") {
    val service = testData.simpleActionService()

    val generatedSrc =
      ActionServiceSourceGenerator.actionProvider(service)
    assertNoDiff(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |package com.example.service;
        |
        |import com.akkaserverless.javasdk.action.ActionCreationContext;
        |import com.akkaserverless.javasdk.action.ActionOptions;
        |import com.akkaserverless.javasdk.action.ActionProvider;
        |import com.akkaserverless.javasdk.impl.action.ActionHandler;
        |import com.external.Empty;
        |import com.external.ExternalDomain;
        |import com.google.protobuf.Descriptors;
        |import java.util.function.Function;
        |
        |/**
        | * MyServiceActionProvider that defines how to register and create the action for
        | * the Protobuf service <code>MyService</code>.
        | *
        | * Should be used with the <code>register</code> method in {@link com.akkaserverless.javasdk.AkkaServerless}.
        | */
        |public class MyServiceActionProvider implements ActionProvider<MyServiceAction> {
        |
        |  private final Function<ActionCreationContext, MyServiceAction> actionFactory;
        |  private final ActionOptions options;
        |
        |  /** Factory method of MyServiceActionProvider */
        |  public static MyServiceActionProvider of(Function<ActionCreationContext, MyServiceAction> actionFactory) {
        |    return new MyServiceActionProvider(actionFactory, ActionOptions.defaults());
        |  }
        |
        |  private MyServiceActionProvider(Function<ActionCreationContext, MyServiceAction> actionFactory, ActionOptions options) {
        |    this.actionFactory = actionFactory;
        |    this.options = options;
        |  }
        |
        |  @Override
        |  public final ActionOptions options() {
        |    return options;
        |  }
        |
        |  public final MyServiceActionProvider withOptions(ActionOptions options) {
        |    return new MyServiceActionProvider(actionFactory, options);
        |  }
        |
        |  @Override
        |  public final Descriptors.ServiceDescriptor serviceDescriptor() {
        |    return ServiceOuterClass.getDescriptor().findServiceByName("MyService");
        |  }
        |
        |  @Override
        |  public final MyServiceActionHandler newHandler(ActionCreationContext context) {
        |    return new MyServiceActionHandler(actionFactory.apply(context));
        |  }
        |
        |  @Override
        |  public final Descriptors.FileDescriptor[] additionalDescriptors() {
        |    return new Descriptors.FileDescriptor[] {
        |      ExternalDomain.getDescriptor(),
        |      ServiceOuterClass.getDescriptor()
        |    };
        |  }
        |
        |}
        |""".stripMargin)
  }

  test("Action Provider source generation 'Action' in the name") {

    val packageNaming = testData.serviceProto().copy(name = "MyServiceAction")
    val service = testData.simpleActionService(packageNaming)

    val generatedSrc =
      ActionServiceSourceGenerator.actionProvider(service)
    assertNoDiff(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |
        |package com.example.service;
        |
        |import com.akkaserverless.javasdk.action.ActionCreationContext;
        |import com.akkaserverless.javasdk.action.ActionOptions;
        |import com.akkaserverless.javasdk.action.ActionProvider;
        |import com.akkaserverless.javasdk.impl.action.ActionHandler;
        |import com.external.Empty;
        |import com.external.ExternalDomain;
        |import com.google.protobuf.Descriptors;
        |import java.util.function.Function;
        |
        |/**
        | * MyServiceActionProvider that defines how to register and create the action for
        | * the Protobuf service <code>MyServiceAction</code>.
        | *
        | * Should be used with the <code>register</code> method in {@link com.akkaserverless.javasdk.AkkaServerless}.
        | */
        |public class MyServiceActionProvider implements ActionProvider<MyServiceActionImpl> {
        |
        |  private final Function<ActionCreationContext, MyServiceActionImpl> actionFactory;
        |  private final ActionOptions options;
        |
        |  /** Factory method of MyServiceActionProvider */
        |  public static MyServiceActionProvider of(Function<ActionCreationContext, MyServiceActionImpl> actionFactory) {
        |    return new MyServiceActionProvider(actionFactory, ActionOptions.defaults());
        |  }
        |
        |  private MyServiceActionProvider(Function<ActionCreationContext, MyServiceActionImpl> actionFactory, ActionOptions options) {
        |    this.actionFactory = actionFactory;
        |    this.options = options;
        |  }
        |
        |  @Override
        |  public final ActionOptions options() {
        |    return options;
        |  }
        |
        |  public final MyServiceActionProvider withOptions(ActionOptions options) {
        |    return new MyServiceActionProvider(actionFactory, options);
        |  }
        |
        |  @Override
        |  public final Descriptors.ServiceDescriptor serviceDescriptor() {
        |    return ServiceOuterClass.getDescriptor().findServiceByName("MyServiceAction");
        |  }
        |
        |  @Override
        |  public final MyServiceActionHandler newHandler(ActionCreationContext context) {
        |    return new MyServiceActionHandler(actionFactory.apply(context));
        |  }
        |
        |  @Override
        |  public final Descriptors.FileDescriptor[] additionalDescriptors() {
        |    return new Descriptors.FileDescriptor[] {
        |      ExternalDomain.getDescriptor(),
        |      ServiceOuterClass.getDescriptor()
        |    };
        |  }
        |
        |}
        |""".stripMargin)
  }

  test("Action with pub/sub source generation") {

    val service = testData.simpleJsonPubSubActionService()

    val generatedSrc =
      ActionServiceSourceGenerator.actionSource(service)
    assertNoDiff(
      generatedSrc,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.action.ActionCreationContext;
        |import com.example.service.domain.EntityOuterClass;
        |import com.google.protobuf.Any;
        |import com.google.protobuf.Empty;
        |
        |/**
        | * An action.
        | *
        | * This class was initially generated based on the .proto definition by Akka Serverless tooling.
        | *
        | * As long as this file exists it will not be overwritten: you can maintain it yourself,
        | * or delete it so it is regenerated as needed.
        | */
        |public class MyServiceAction extends AbstractMyServiceAction {
        |
        |  public MyServiceAction(ActionCreationContext creationContext) {}
        |
        |  /** Handler for "InFromTopic". */
        |  @Override
        |  public Effect<Empty> inFromTopic(Any any) {
        |    // JSON input from a topic can be decoded using JsonSupport.decodeJson(MyClass.class, any)
        |    throw new RuntimeException("The command handler for `InFromTopic` is not implemented, yet");
        |  }
        |  /** Handler for "OutToTopic". */
        |  @Override
        |  public Effect<Any> outToTopic(EntityOuterClass.EntityUpdated entityUpdated) {
        |    // JSON output to emit to a topic can be encoded using JsonSupport.encodeJson(myPojo)
        |    throw new RuntimeException("The command handler for `OutToTopic` is not implemented, yet");
        |  }
        |}
        |""".stripMargin)
  }
}
