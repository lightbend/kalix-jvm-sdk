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

package com.akkaserverless.codegen.scalasdk

import com.akkaserverless.codegen.scalasdk.impl.ViewServiceSourceGenerator
import com.lightbend.akkasls.codegen.TestData

class ViewServiceSourceGeneratorSuite extends munit.FunSuite {

  test("source, transform_updates=true") {
    val service = TestData.simpleViewService()

    val packageName = "com.example.service"
    val generatedSrc =
      ViewServiceSourceGenerator.viewSource(service, packageName)
    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.scalasdk.view.View.UpdateEffect
         |import com.akkaserverless.scalasdk.view.ViewContext
         |import com.example.service.domain.EntityOuterClass
         |
         |class MyServiceViewImpl(context: ViewContext) extends AbstractMyServiceView {
         |
         |  override def emptyState: ServiceOuterClass.ViewState =
         |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state")
         |
         |  override def created(
         |    state: ServiceOuterClass.ViewState, entityCreated: EntityOuterClass.EntityCreated): UpdateEffect[ServiceOuterClass.ViewState] =
         |    throw new UnsupportedOperationException("Update handler for 'Created' not implemented yet")
         |
         |  override def updated(
         |    state: ServiceOuterClass.ViewState, entityUpdated: EntityOuterClass.EntityUpdated): UpdateEffect[ServiceOuterClass.ViewState] =
         |    throw new UnsupportedOperationException("Update handler for 'Updated' not implemented yet")
         |}
         |""".stripMargin)
  }

  test("source, transform_updates=false") {
    val service = TestData.simpleViewService().copy(transformedUpdates = Nil)

    val packageName = "com.example.service"
    val generatedSrc =
      ViewServiceSourceGenerator.viewSource(service, packageName)
    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.scalasdk.view.View.UpdateEffect
         |import com.akkaserverless.scalasdk.view.ViewContext
         |import com.example.service.domain.EntityOuterClass
         |
         |class MyServiceViewImpl(context: ViewContext) extends AbstractMyServiceView {
         |
         |
         |  
         |}
         |""".stripMargin)
  }

  test("abstract source, transform_updates=true") {
    val service = TestData.simpleViewService()
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.abstractView(service, packageName)
    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.scalasdk.view.View
         |import com.example.service.domain.EntityOuterClass
         |
         |abstract class AbstractMyServiceView extends View[ServiceOuterClass.ViewState] {
         |
         |
         |  def created(
         |    state: ServiceOuterClass.ViewState, entityCreated: EntityOuterClass.EntityCreated): View.UpdateEffect[ServiceOuterClass.ViewState]
         |  def updated(
         |    state: ServiceOuterClass.ViewState, entityUpdated: EntityOuterClass.EntityUpdated): View.UpdateEffect[ServiceOuterClass.ViewState]
         |}
         |""".stripMargin)
  }

  test("abstract source, transform_updates=false") {
    val service = TestData.simpleViewService().copy(transformedUpdates = Nil)
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.abstractView(service, packageName)
    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.scalasdk.view.View
         |import com.example.service.domain.EntityOuterClass
         |
         |abstract class AbstractMyServiceView extends View[ServiceOuterClass.ViewState] {
         |
         |  override def emptyState: ServiceOuterClass.ViewState =
         |    null // emptyState is only used with transform_updates=true
         |
         |  
         |}
         |""".stripMargin)
  }

  test("handler source") {
    val service = TestData.simpleViewService()
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.viewHandler(service, packageName)

    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound
         |import com.akkaserverless.scalasdk.impl.view.ViewHandler
         |import com.akkaserverless.scalasdk.view.View
         |import com.example.service.domain.EntityOuterClass
         |
         |/** A view handler */
         |class MyServiceViewHandler(view: MyServiceViewImpl) extends ViewHandler[ServiceOuterClass.ViewState, MyServiceViewImpl](view) {
         |
         |  override def handleUpdate(
         |      eventName: String,
         |      state: ServiceOuterClass.ViewState,
         |      event: Any): View.UpdateEffect[ServiceOuterClass.ViewState] = {
         |
         |    eventName match {
         |      case "Created" =>
         |        view.created(
         |            state,
         |            event.asInstanceOf[EntityOuterClass.EntityCreated])
         |
         |      case "Updated" =>
         |        view.updated(
         |            state,
         |            event.asInstanceOf[EntityOuterClass.EntityUpdated])
         |
         |      case _ =>
         |        throw new UpdateHandlerNotFound(eventName)
         |    }
         |  }
         |
         |}
         |""".stripMargin)
  }

  test("provider source") {
    val service = TestData.simpleViewService()
    val packageName = "com.example.service"

    val generatedSrc =
      ViewServiceSourceGenerator.viewProvider(service, packageName)

    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound
         |import com.akkaserverless.scalasdk.impl.view.ViewHandler
         |import com.akkaserverless.scalasdk.view.View
         |import com.akkaserverless.scalasdk.view.ViewCreationContext
         |import com.akkaserverless.scalasdk.view.ViewProvider
         |import com.example.service.MyServiceViewImpl
         |import com.google.protobuf.Descriptors
         |import com.google.protobuf.EmptyProto
         |import scala.collection.immutable
         |
         |object MyServiceViewProvider {
         |  def apply(viewFactory: Function[ViewCreationContext, MyServiceViewImpl]): MyServiceViewProvider =
         |    new MyServiceViewProvider(viewFactory, viewId = "MyService")
         |}
         |
         |class MyServiceViewProvider private(viewFactory: Function[ViewCreationContext, MyServiceViewImpl],
         |    override val viewId: String)
         |  extends ViewProvider[ServiceOuterClass.ViewState, MyServiceViewImpl] {
         |
         |  /**
         |   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
         |   * A different identifier can be needed when making rolling updates with changes to the view definition.
         |   */
         |  def withViewId(viewId: String): MyServiceViewProvider =
         |    new MyServiceViewProvider(viewFactory, viewId)
         |
         |  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
         |    ServiceOuterClass.getDescriptor().findServiceByName("MyService")
         |
         |  override final def newHandler(context: ViewCreationContext): MyServiceViewHandler =
         |    new MyServiceViewHandler(viewFactory(context))
         |
         |  override final def additionalDescriptors: immutable.Seq[Descriptors.FileDescriptor] =
         |    ServiceOuterClass.getDescriptor() ::
         |    Nil
         |}
         |""".stripMargin)
  }
}
