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

class ViewServiceSourceGeneratorSuite extends munit.FunSuite {
  private val testData = TestData.javaStyle

  test("source, transform_updates=true") {
    val service = testData.simpleViewService()

    val packageName = "com.example.service"
    val generatedSrc =
      ViewServiceSourceGenerator.viewSource(service, packageName)
    assertNoDiff(
      generatedSrc,
      """package com.example.service;
        |
        |import com.akkaserverless.javasdk.view.ViewContext;
        |import com.example.service.domain.EntityOuterClass;
        |
        |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
        |//
        |// As long as this file exists it will not be overwritten: you can maintain it yourself,
        |// or delete it so it is regenerated as needed.
        |
        |public class MyServiceViewImpl extends AbstractMyServiceView {
        |
        |  public MyServiceViewImpl(ViewContext context) {}
        |
        |  @Override
        |  public ServiceOuterClass.ViewState emptyState() {
        |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state");
        |  }
        |
        |  @Override
        |  public UpdateEffect<ServiceOuterClass.ViewState> created(
        |    ServiceOuterClass.ViewState state, EntityOuterClass.EntityCreated entityCreated) {
        |    throw new UnsupportedOperationException("Update handler for 'Created' not implemented yet");
        |  }
        |  @Override
        |  public UpdateEffect<ServiceOuterClass.ViewState> updated(
        |    ServiceOuterClass.ViewState state, EntityOuterClass.EntityUpdated entityUpdated) {
        |    throw new UnsupportedOperationException("Update handler for 'Updated' not implemented yet");
        |  }
        |}
        |""".stripMargin)
  }

  test("source, transform_updates=false") {
    val service = testData.simpleViewService().copy(transformedUpdates = Nil)

    val packageName = "com.example.service"
    val generatedSrc =
      ViewServiceSourceGenerator.viewSource(service, packageName)
    assertNoDiff(
      generatedSrc,
      """package com.example.service;
         |
         |import com.akkaserverless.javasdk.view.ViewContext;
         |import com.example.service.domain.EntityOuterClass;
         |
         |// This class was initially generated based on the .proto definition by Akka Serverless tooling.
         |//
         |// As long as this file exists it will not be overwritten: you can maintain it yourself,
         |// or delete it so it is regenerated as needed.
         |
         |public class MyServiceViewImpl extends AbstractMyServiceView {
         |
         |  public MyServiceViewImpl(ViewContext context) {}
         |
         |  
         |}
         |""".stripMargin)
  }

  test("abstract source, transform_updates=true") {
    val service = testData.simpleViewService()
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.abstractView(service, packageName)
    assertNoDiff(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |package com.example.service;
        |
        |import com.akkaserverless.javasdk.view.View;
        |import com.example.service.domain.EntityOuterClass;
        |
        |public abstract class AbstractMyServiceView extends View<ServiceOuterClass.ViewState> {
        |
        |  public abstract UpdateEffect<ServiceOuterClass.ViewState> created(
        |    ServiceOuterClass.ViewState state, EntityOuterClass.EntityCreated entityCreated);
        |  public abstract UpdateEffect<ServiceOuterClass.ViewState> updated(
        |    ServiceOuterClass.ViewState state, EntityOuterClass.EntityUpdated entityUpdated);
        |}
        |""".stripMargin)
  }

  test("abstract source, transform_updates=false") {
    val service = testData.simpleViewService().copy(transformedUpdates = Nil)
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.abstractView(service, packageName)
    assertNoDiff(
      generatedSrc,
      """|/* This code is managed by Akka Serverless tooling.
         | * It will be re-generated to reflect any changes to your protobuf definitions.
         | * DO NOT EDIT
         | */
         |package com.example.service;
         |
         |import com.akkaserverless.javasdk.view.View;
         |import com.example.service.domain.EntityOuterClass;
         |
         |public abstract class AbstractMyServiceView extends View<ServiceOuterClass.ViewState> {
         |
         |  @Override
         |  public ServiceOuterClass.ViewState emptyState() {
         |    return null; // emptyState is only used with transform_updates=true
         |  }
         |
         |  
         |}
         |""".stripMargin)
  }

  test("handler source") {
    val service = testData.simpleViewService()
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.viewHandler(service, packageName)

    assertNoDiff(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |package com.example.service;
        |
        |import com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound;
        |import com.akkaserverless.javasdk.impl.view.ViewHandler;
        |import com.akkaserverless.javasdk.view.View;
        |import com.example.service.domain.EntityOuterClass;
        |
        |/** A view handler */
        |public class MyServiceViewHandler extends ViewHandler<ServiceOuterClass.ViewState, MyServiceViewImpl> {
        |
        |  public MyServiceViewHandler(MyServiceViewImpl view) {
        |    super(view);
        |  }
        |
        |  @Override
        |  public View.UpdateEffect<ServiceOuterClass.ViewState> handleUpdate(
        |      String eventName,
        |      ServiceOuterClass.ViewState state,
        |      Object event) {
        |
        |    switch (eventName) {
        |      case "Created":
        |        return view().created(
        |            state,
        |            (EntityOuterClass.EntityCreated) event);
        |
        |      case "Updated":
        |        return view().updated(
        |            state,
        |            (EntityOuterClass.EntityUpdated) event);
        |
        |      default:
        |        throw new UpdateHandlerNotFound(eventName);
        |    }
        |  }
        |
        |}
        |""".stripMargin)
  }

  test("provider source") {
    val service = testData.simpleViewService()
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.viewProvider(service, packageName)

    assertNoDiff(
      generatedSrc,
      """|/* This code is managed by Akka Serverless tooling.
       | * It will be re-generated to reflect any changes to your protobuf definitions.
       | * DO NOT EDIT
       | */
       |package com.example.service;
       |
       |import com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound;
       |import com.akkaserverless.javasdk.impl.view.ViewHandler;
       |import com.akkaserverless.javasdk.view.View;
       |import com.akkaserverless.javasdk.view.ViewCreationContext;
       |import com.akkaserverless.javasdk.view.ViewOptions;
       |import com.akkaserverless.javasdk.view.ViewProvider;
       |import com.google.protobuf.Descriptors;
       |import com.google.protobuf.EmptyProto;
       |import java.util.function.Function;
       |
       |public class MyServiceViewProvider implements ViewProvider<ServiceOuterClass.ViewState, MyServiceViewImpl> {
       |
       |  private final Function<ViewCreationContext, MyServiceViewImpl> viewFactory;
       |  private final String viewId;
       |  private final ViewOptions options;
       |
       |  /** Factory method of MyServiceViewImpl */
       |  public static MyServiceViewProvider of(
       |      Function<ViewCreationContext, MyServiceViewImpl> viewFactory) {
       |    return new MyServiceViewProvider(viewFactory, "MyService", ViewOptions.defaults());
       |  }
       |
       |  private MyServiceViewProvider(
       |      Function<ViewCreationContext, MyServiceViewImpl> viewFactory,
       |      String viewId,
       |      ViewOptions options) {
       |    this.viewFactory = viewFactory;
       |    this.viewId = viewId;
       |    this.options = options;
       |  }
       |
       |  @Override
       |  public String viewId() {
       |    return viewId;
       |  }
       |
       |  @Override
       |  public final ViewOptions options() {
       |    return options;
       |  }
       |
       |  public final MyServiceViewProvider withOptions(ViewOptions options) {
       |    return new MyServiceViewProvider(viewFactory, viewId, options);
       |  }
       |
       |  /**
       |   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
       |   * A different identifier can be needed when making rolling updates with changes to the view definition.
       |   */
       |  public MyServiceViewProvider withViewId(String viewId) {
       |    return new MyServiceViewProvider(viewFactory, viewId, options);
       |  }
       |
       |  @Override
       |  public final Descriptors.ServiceDescriptor serviceDescriptor() {
       |    return ServiceOuterClass.getDescriptor().findServiceByName("MyService");
       |  }
       |
       |  @Override
       |  public final MyServiceViewHandler newHandler(ViewCreationContext context) {
       |    return new MyServiceViewHandler(viewFactory.apply(context));
       |  }
       |
       |  @Override
       |  public final Descriptors.FileDescriptor[] additionalDescriptors() {
       |    return new Descriptors.FileDescriptor[] {ServiceOuterClass.getDescriptor()};
       |  }
       |}
       |""".stripMargin)
  }
}
