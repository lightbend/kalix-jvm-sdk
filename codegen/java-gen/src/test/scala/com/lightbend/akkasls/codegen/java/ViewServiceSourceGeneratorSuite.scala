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

  test("source") {

    val service = TestData.simpleViewService()

    val packageName = "com.example.service"
    val generatedSrc =
      ViewServiceSourceGenerator.viewSource(
        service,
        packageName
      )
    assertEquals(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |package com.example.service;
        |
        |import com.akkaserverless.javasdk.view.View;
        |import com.akkaserverless.javasdk.view.ViewContext;
        |import com.example.service.persistence.EntityOuterClass;
        |import java.util.function.Function;
        |
        |public class MyServiceView extends AbstractMyServiceView {
        |
        |  public MyServiceView(ViewContext context) {}
        |
        |  @Override
        |  public ServiceOuterClass.ViewState emptyState() {
        |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state");
        |  }
        |
        |  @Override
        |  public UpdateEffect<ServiceOuterClass.ViewState> created(
        |    ServiceOuterClass.ViewState state, EntityOuterClass.EntityCreated created) {
        |    throw new UnsupportedOperationException("Update handler for 'Created' not implemented yet");
        |  }
        |  @Override
        |  public UpdateEffect<ServiceOuterClass.ViewState> updated(
        |    ServiceOuterClass.ViewState state, EntityOuterClass.EntityUpdated updated) {
        |    throw new UnsupportedOperationException("Update handler for 'Updated' not implemented yet");
        |  }
        |}""".stripMargin
    )
  }

  test("interface source") {
    val service = TestData.simpleViewService()
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.abstractView(service, packageName)
    assertEquals(
      generatedSrc,
      """/* This code is managed by Akka Serverless tooling.
        | * It will be re-generated to reflect any changes to your protobuf definitions.
        | * DO NOT EDIT
        | */
        |package com.example.service;
        |
        |import com.akkaserverless.javasdk.view.View;
        |import com.example.service.persistence.EntityOuterClass;
        |import java.util.function.Function;
        |
        |public abstract class AbstractMyServiceView extends View<ServiceOuterClass.ViewState> {
        |
        |  public abstract UpdateEffect<ServiceOuterClass.ViewState> created(
        |    ServiceOuterClass.ViewState state, EntityOuterClass.EntityCreated created);
        |  public abstract UpdateEffect<ServiceOuterClass.ViewState> updated(
        |    ServiceOuterClass.ViewState state, EntityOuterClass.EntityUpdated updated);
        |}""".stripMargin
    )
  }

  test("handler source") {
    val service = TestData.simpleViewService()
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.viewHandler(service, packageName)

    assertEquals(
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
        |import com.example.service.persistence.EntityOuterClass;
        |
        |/** A view handler */
        |public class MyServiceViewHandler extends ViewHandler<ServiceOuterClass.ViewState, MyServiceView> {
        |
        |  public MyServiceViewHandler(MyServiceView view) {
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
        |}""".stripMargin
    )
  }

  test("provider source") {
    val service = TestData.simpleViewService()
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.viewProvider(service, packageName)

    assertEquals(
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
       |import com.akkaserverless.javasdk.view.ViewProvider;
       |import com.example.service.MyServiceView;
       |import com.google.protobuf.Descriptors;
       |import com.google.protobuf.EmptyProto;
       |import java.util.function.Function;
       |
       |public class MyServiceViewProvider implements ViewProvider {
       |
       |  private final Function<ViewCreationContext, MyServiceView> viewFactory;
       |
       |  /** Factory method of MyServiceView */
       |  public static MyServiceViewProvider of(
       |      Function<ViewCreationContext, MyServiceView> viewFactory) {
       |    return new MyServiceViewProvider(viewFactory);
       |  }
       |
       |  private MyServiceViewProvider(
       |      Function<ViewCreationContext, MyServiceView> viewFactory) {
       |    this.viewFactory = viewFactory;
       |  }
       |
       |  @Override
       |  public String viewId() {
       |    return "my-view-id";
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
       |}""".stripMargin
    )
  }
}
