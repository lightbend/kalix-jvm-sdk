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

package kalix.scalasdk.impl.view

import akka.stream.Materializer
import kalix.javasdk
import kalix.javasdk.view.ViewOptions
import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.MetadataConverters
import kalix.scalasdk.view.UpdateContext
import kalix.scalasdk.view.View
import kalix.scalasdk.view.ViewCreationContext
import kalix.scalasdk.view.ViewProvider
import com.google.protobuf.Descriptors

import java.util.Optional
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.OptionConverters._

private[scalasdk] final class JavaViewAdapter(scalaSdkView: View) extends javasdk.view.View {
  override def emptyState(): Any = scalaSdkView.emptyState

  override def _internalSetUpdateContext(context: Optional[javasdk.view.UpdateContext]): Unit =
    scalaSdkView._internalSetUpdateContext(context.map(new ScalaUpdateContextAdapter(_)).toScala)
}

private[scalasdk] final class JavaViewProviderAdapter[V <: View](scalaSdkProvider: ViewProvider[V])
    extends javasdk.view.ViewProvider[javasdk.view.View] {
  override def serviceDescriptor(): Descriptors.ServiceDescriptor =
    scalaSdkProvider.serviceDescriptor

  override def viewId(): String =
    scalaSdkProvider.viewId

  override def options(): ViewOptions =
    javasdk.impl.view.ViewOptionsImpl(scalaSdkProvider.options.forwardHeaders.asJava)

  override def newRouter(context: javasdk.view.ViewCreationContext): javasdk.impl.view.ViewRouter[javasdk.view.View] = {
    val scalaSdkRouter = scalaSdkProvider.newRouter(new ScalaViewCreationContextAdapter(context))
    new JavaViewRouterAdapter(new JavaViewAdapter(scalaSdkRouter.view), scalaSdkRouter)
  }

  override def additionalDescriptors(): Array[Descriptors.FileDescriptor] =
    scalaSdkProvider.additionalDescriptors.toArray
}

private[scalasdk] class JavaViewRouterAdapter[V <: View](javaSdkView: javasdk.view.View, scalaSdkHandler: ViewRouter[V])
    extends javasdk.impl.view.ViewRouter(javaSdkView) {

  override def handleUpdate[S](commandName: String, state: S, event: Any): javasdk.view.View.UpdateEffect[S] = {
    scalaSdkHandler.handleUpdate(commandName, state, event) match {
      case effect: ViewUpdateEffectImpl.PrimaryUpdateEffect[S] => effect.toJavaSdk
    }
  }

  override def viewTable(commandName: String, event: Any): String = scalaSdkHandler.viewTable(commandName, event)
}

private[scalasdk] final class ScalaViewCreationContextAdapter(javaSdkContext: javasdk.view.ViewCreationContext)
    extends ViewCreationContext {
  override def viewId: String =
    javaSdkContext.viewId()

  override def materializer(): Materializer = javaSdkContext.materializer()
}

private[scalasdk] final class ScalaUpdateContextAdapter(val javaSdkContext: javasdk.view.UpdateContext)
    extends UpdateContext {
  override def eventSubject: Option[String] =
    javaSdkContext.eventSubject().toScala

  override def eventName: String =
    javaSdkContext.eventName()

  override def metadata: Metadata =
    MetadataConverters.toScala(javaSdkContext.metadata())

  override def viewId: String =
    javaSdkContext.viewId()

  override def viewTable: String =
    javaSdkContext.viewTable()

  override def materializer(): Materializer = javaSdkContext.materializer()
}
