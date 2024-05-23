/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk.impl.view

import akka.stream.Materializer
import com.google.protobuf.Descriptors
import kalix.javasdk
import kalix.javasdk.view.ViewOptions
import kalix.scalasdk.Metadata
import kalix.scalasdk.impl.MetadataConverters
import kalix.scalasdk.view.UpdateContext
import kalix.scalasdk.view.View
import kalix.scalasdk.view.ViewCreationContext
import kalix.scalasdk.view.ViewProvider

import java.util.Optional
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.OptionConverters._

private[scalasdk] final class JavaViewAdapter[S](scalaSdkView: View[S]) extends javasdk.view.View[S] {
  override def emptyState(): S = scalaSdkView.emptyState

  override def _internalSetUpdateContext(context: Optional[javasdk.view.UpdateContext]): Unit =
    scalaSdkView._internalSetUpdateContext(context.map(new ScalaUpdateContextAdapter(_)).toScala)
}

private[scalasdk] final class JavaViewProviderAdapter(scalaSdkProvider: ViewProvider)
    extends javasdk.view.ViewProvider {
  override def serviceDescriptor(): Descriptors.ServiceDescriptor =
    scalaSdkProvider.serviceDescriptor

  override def viewId(): String =
    scalaSdkProvider.viewId

  override def options(): ViewOptions =
    javasdk.impl.view.ViewOptionsImpl(scalaSdkProvider.options.forwardHeaders.asJava)

  override def newRouter(context: javasdk.view.ViewCreationContext): javasdk.impl.view.ViewUpdateRouter = {
    scalaSdkProvider.newRouter(new ScalaViewCreationContextAdapter(context)) match {
      case scalaSdkRouter: ViewRouter[_, _]          => JavaViewRouterAdapter(scalaSdkRouter)
      case scalaSdkMultiRouter: ViewMultiTableRouter => JavaViewMultiTableRouterAdapter(scalaSdkMultiRouter)
    }
  }

  override def additionalDescriptors(): Array[Descriptors.FileDescriptor] =
    scalaSdkProvider.additionalDescriptors.toArray
}

private[scalasdk] object JavaViewRouterAdapter {
  def apply[S](scalaSdkRouter: ViewRouter[_, _]): JavaViewRouterAdapter[S] = {
    val typedRouter = scalaSdkRouter.asInstanceOf[ViewRouter[S, View[S]]]
    new JavaViewRouterAdapter[S](new JavaViewAdapter[S](typedRouter.view), typedRouter)
  }
}

private[scalasdk] class JavaViewRouterAdapter[S](
    javaSdkView: javasdk.view.View[S],
    scalaSdkRouter: ViewRouter[S, View[S]])
    extends javasdk.impl.view.ViewRouter[S, javasdk.view.View[S]](javaSdkView) {

  override def handleUpdate(commandName: String, state: S, event: Any): javasdk.view.View.UpdateEffect[S] = {
    scalaSdkRouter.handleUpdate(commandName, state, event) match {
      case effect: ViewUpdateEffectImpl.PrimaryUpdateEffect[S @unchecked] => effect.toJavaSdk
    }
  }
}

private[scalasdk] object JavaViewMultiTableRouterAdapter {
  def apply[V](scalaSdkRouter: ViewMultiTableRouter): JavaViewMultiTableRouterAdapter =
    new JavaViewMultiTableRouterAdapter(scalaSdkRouter)
}

private[scalasdk] class JavaViewMultiTableRouterAdapter(scalaSdkMultiRouter: ViewMultiTableRouter)
    extends javasdk.impl.view.ViewMultiTableRouter {

  override def viewRouter(eventName: String): javasdk.impl.view.ViewRouter[_, _] = {
    val scalaSdkRouter = scalaSdkMultiRouter.viewRouter(eventName)
    JavaViewRouterAdapter(scalaSdkRouter)
  }
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

  override def materializer(): Materializer = javaSdkContext.materializer()
}
