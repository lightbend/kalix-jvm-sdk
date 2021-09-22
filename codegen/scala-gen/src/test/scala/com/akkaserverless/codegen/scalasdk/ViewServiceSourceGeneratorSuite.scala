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
  private val testData = TestData(TestData.defaultPackageNamingTemplate.copy(javaOuterClassnameOption = None))

  test("source, transform_updates=true") {
    val service = testData.simpleViewService()

    val generatedSrc = ViewServiceSourceGenerator.viewSource(service)
    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.scalasdk.view.View.UpdateEffect
         |import com.akkaserverless.scalasdk.view.ViewContext
         |import com.example.service.domain.EntityCreated
         |import com.example.service.domain.EntityUpdated
         |
         |class MyServiceViewImpl(context: ViewContext) extends AbstractMyServiceView {
         |
         |  override def emptyState: ViewState =
         |    throw new UnsupportedOperationException("Not implemented yet, replace with your empty view state")
         |
         |  override def created(
         |    state: ViewState, entityCreated: EntityCreated): UpdateEffect[ViewState] =
         |    throw new UnsupportedOperationException("Update handler for 'Created' not implemented yet")
         |
         |  override def updated(
         |    state: ViewState, entityUpdated: EntityUpdated): UpdateEffect[ViewState] =
         |    throw new UnsupportedOperationException("Update handler for 'Updated' not implemented yet")
         |}
         |""".stripMargin)
  }

  test("source, transform_updates=false") {
    val service = testData.simpleViewService().copy(transformedUpdates = Nil)

    val generatedSrc = ViewServiceSourceGenerator.viewSource(service)
    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.scalasdk.view.View.UpdateEffect
         |import com.akkaserverless.scalasdk.view.ViewContext
         |import com.example.service.domain.EntityCreated
         |import com.example.service.domain.EntityUpdated
         |
         |class MyServiceViewImpl(context: ViewContext) extends AbstractMyServiceView {
         |
         |
         |  
         |}
         |""".stripMargin)
  }

  test("abstract source, transform_updates=true") {
    val service = testData.simpleViewService()

    val generatedSrc = ViewServiceSourceGenerator.abstractView(service)
    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.scalasdk.view.View
         |import com.example.service.domain.EntityCreated
         |import com.example.service.domain.EntityUpdated
         |
         |abstract class AbstractMyServiceView extends View[ViewState] {
         |
         |
         |  def created(
         |    state: ViewState, entityCreated: EntityCreated): View.UpdateEffect[ViewState]
         |  def updated(
         |    state: ViewState, entityUpdated: EntityUpdated): View.UpdateEffect[ViewState]
         |}
         |""".stripMargin)
  }

  test("abstract source, transform_updates=false") {
    val service = testData.simpleViewService().copy(transformedUpdates = Nil)

    val generatedSrc = ViewServiceSourceGenerator.abstractView(service)
    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.scalasdk.view.View
         |import com.example.service.domain.EntityCreated
         |import com.example.service.domain.EntityUpdated
         |
         |abstract class AbstractMyServiceView extends View[ViewState] {
         |
         |  override def emptyState: ViewState =
         |    null // emptyState is only used with transform_updates=true
         |
         |  
         |}
         |""".stripMargin)
  }

  test("handler source") {
    val service = testData.simpleViewService()

    val generatedSrc = ViewServiceSourceGenerator.viewHandler(service)

    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound
         |import com.akkaserverless.scalasdk.impl.view.ViewHandler
         |import com.akkaserverless.scalasdk.view.View
         |import com.example.service.domain.EntityCreated
         |import com.example.service.domain.EntityUpdated
         |
         |/** A view handler */
         |class MyServiceViewHandler(view: MyServiceViewImpl)
         |  extends ViewHandler[ViewState, MyServiceViewImpl](view) {
         |
         |  override def handleUpdate(
         |      eventName: String,
         |      state: ViewState,
         |      event: Any): View.UpdateEffect[ViewState] = {
         |
         |    eventName match {
         |      case "Created" =>
         |        view.created(
         |            state,
         |            event.asInstanceOf[EntityCreated])
         |
         |      case "Updated" =>
         |        view.updated(
         |            state,
         |            event.asInstanceOf[EntityUpdated])
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
    val service = testData.simpleViewService()

    val generatedSrc = ViewServiceSourceGenerator.viewProvider(service)

    assertNoDiff(
      generatedSrc,
      """|package com.example.service
         |
         |import com.akkaserverless.javasdk.impl.view.UpdateHandlerNotFound
         |import com.akkaserverless.scalasdk.impl.view.ViewHandler
         |import com.akkaserverless.scalasdk.view.View
         |import com.akkaserverless.scalasdk.view.ViewCreationContext
         |import com.akkaserverless.scalasdk.view.ViewOptions
         |import com.akkaserverless.scalasdk.view.ViewProvider
         |import com.google.protobuf.Descriptors
         |import com.google.protobuf.EmptyProto
         |import scala.collection.immutable
         |
         |object MyServiceViewProvider {
         |  def apply(viewFactory: Function[ViewCreationContext, MyServiceViewImpl]): MyServiceViewProvider =
         |    new MyServiceViewProvider(viewFactory, viewId = "MyService", options = ViewOptions.defaults)
         |}
         |
         |class MyServiceViewProvider private(
         |    viewFactory: Function[ViewCreationContext, MyServiceViewImpl],
         |    override val viewId: String,
         |    override val options: ViewOptions)
         |  extends ViewProvider[ViewState, MyServiceViewImpl] {
         |
         |  /**
         |   * Use a custom view identifier. By default, the viewId is the same as the proto service name.
         |   * A different identifier can be needed when making rolling updates with changes to the view definition.
         |   */
         |  def withViewId(viewId: String): MyServiceViewProvider =
         |    new MyServiceViewProvider(viewFactory, viewId, options)
         |
         |  def withOptions(newOptions: ViewOptions): MyServiceViewProvider =
         |    new MyServiceViewProvider(viewFactory, viewId, newOptions)
         |
         |  override final def serviceDescriptor: Descriptors.ServiceDescriptor =
         |    MyServiceProto.javaDescriptor.findServiceByName("MyService")
         |
         |  override final def newHandler(context: ViewCreationContext): MyServiceViewHandler =
         |    new MyServiceViewHandler(viewFactory(context))
         |
         |  override final def additionalDescriptors: immutable.Seq[Descriptors.FileDescriptor] =
         |    MyServiceProto.javaDescriptor ::
         |    Nil
         |}
         |""".stripMargin)
  }
}
