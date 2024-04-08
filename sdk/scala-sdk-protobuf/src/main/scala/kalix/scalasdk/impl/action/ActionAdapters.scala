/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.scalasdk.impl.action

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.javadsl.Source
import kalix.javasdk
import kalix.javasdk.impl.action.ActionOptionsImpl
import kalix.scalasdk.Metadata
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionContext
import kalix.scalasdk.action.ActionCreationContext
import kalix.scalasdk.action.ActionProvider
import kalix.scalasdk.action.MessageEnvelope
import kalix.scalasdk.impl.InternalContext
import kalix.scalasdk.impl.MetadataConverters
import com.google.protobuf.Descriptors
import io.opentelemetry.api.trace.Tracer
import kalix.javasdk.impl.telemetry.Telemetry
import kalix.protocol.component.MetadataEntry
import kalix.scalasdk.impl.MetadataImpl

import java.util.Optional
import scala.jdk.CollectionConverters.SetHasAsJava
import scala.jdk.OptionConverters.RichOptional

private[scalasdk] final case class JavaActionAdapter(scalaSdkAction: Action) extends javasdk.action.Action {

  /** INTERNAL API */
  override def _internalSetActionContext(context: Optional[javasdk.action.ActionContext]): Unit =
    scalaSdkAction._internalSetActionContext(context.map(new ScalaActionContextAdapter(_)).toScala)
}

private[scalasdk] final case class JavaActionProviderAdapter[A <: Action](scalaSdkProvider: ActionProvider[A])
    extends javasdk.action.ActionProvider[javasdk.action.Action] {

  override def options(): javasdk.action.ActionOptions =
    ActionOptionsImpl(scalaSdkProvider.options.forwardHeaders.asJava)

  override def newRouter(
      javaSdkContext: javasdk.action.ActionCreationContext): javasdk.impl.action.ActionRouter[javasdk.action.Action] = {
    val scalaSdkRouter = scalaSdkProvider.newRouter(ScalaActionCreationContextAdapter(javaSdkContext))
    JavaActionRouterAdapter(JavaActionAdapter(scalaSdkRouter.action), scalaSdkRouter)
  }

  override def serviceDescriptor(): Descriptors.ServiceDescriptor =
    scalaSdkProvider.serviceDescriptor

  override def additionalDescriptors(): Array[Descriptors.FileDescriptor] =
    scalaSdkProvider.additionalDescriptors.toArray
}

private[scalasdk] final case class JavaActionRouterAdapter[A <: Action](
    javaSdkAction: javasdk.action.Action,
    scalaSdkRouter: ActionRouter[A])
    extends javasdk.impl.action.ActionRouter[javasdk.action.Action](javaSdkAction) {

  override def handleUnary(
      commandName: String,
      message: javasdk.action.MessageEnvelope[Any]): javasdk.action.Action.Effect[_] = {

    val messageEnvelopeAdapted = ScalaMessageEnvelopeAdapter(message)
    scalaSdkRouter.handleUnary(commandName, messageEnvelopeAdapted) match {
      case eff: ActionEffectImpl.PrimaryEffect[_] => eff.toJavaSdk
    }
  }
  override def handleStreamedOut(
      commandName: String,
      message: javasdk.action.MessageEnvelope[Any]): Source[javasdk.action.Action.Effect[_], NotUsed] = {

    val messageEnvelopeAdapted = ScalaMessageEnvelopeAdapter(message)
    val src = scalaSdkRouter.handleStreamedOut(commandName, messageEnvelopeAdapted)

    src.map { case eff: ActionEffectImpl.PrimaryEffect[_] =>
      eff.toJavaSdk
    }.asJava
  }

  override def handleStreamedIn(
      commandName: String,
      stream: Source[javasdk.action.MessageEnvelope[Any], NotUsed]): javasdk.action.Action.Effect[_] = {

    val convertedStream =
      stream
        .map(el => ScalaMessageEnvelopeAdapter(el))
        .asScala

    scalaSdkRouter.handleStreamedIn(commandName, convertedStream) match {
      case eff: ActionEffectImpl.PrimaryEffect[_] => eff.toJavaSdk
    }
  }

  override def handleStreamed(commandName: String, stream: Source[javasdk.action.MessageEnvelope[Any], NotUsed])
      : Source[javasdk.action.Action.Effect[_], NotUsed] = {

    val convertedStream =
      stream
        .map(el => ScalaMessageEnvelopeAdapter(el).asInstanceOf[MessageEnvelope[Any]])
        .asScala

    scalaSdkRouter
      .handleStreamed(commandName, convertedStream)
      .map { case eff: ActionEffectImpl.PrimaryEffect[_] =>
        eff.toJavaSdk
      }
      .asJava
  }

}

private[scalasdk] final case class ScalaMessageEnvelopeAdapter[A](javaSdkMsgEnvelope: javasdk.action.MessageEnvelope[A])
    extends MessageEnvelope[A] {

  override def metadata: Metadata =
    MetadataConverters.toScala(javaSdkMsgEnvelope.metadata())

  override def payload: A = javaSdkMsgEnvelope.payload()
}

private[scalasdk] final case class ScalaActionCreationContextAdapter(
    javaSdkCreationContext: javasdk.action.ActionCreationContext)
    extends ActionCreationContext {

  override def getGrpcClient[T](clientClass: Class[T], service: String): T =
    javaSdkCreationContext.getGrpcClient(clientClass, service)

  override def materializer(): Materializer = javaSdkCreationContext.materializer()

  def getOpenTelemetryTracer: Option[Tracer] = javaSdkCreationContext.getOpenTelemetryTracer.toScala

  override def getTracer: Tracer = javaSdkCreationContext.getTracer
}

private[scalasdk] final case class ScalaActionContextAdapter(javaSdkContext: javasdk.action.ActionContext)
    extends ActionContext
    with InternalContext {

  override def metadata: Metadata =
    MetadataConverters.toScala(javaSdkContext.metadata())

  override def eventSubject: Option[String] =
    javaSdkContext.eventSubject().toScala

  def getComponentGrpcClient[T](serviceClass: Class[T]): T = javaSdkContext match {
    case ctx: javasdk.impl.AbstractContext => ctx.getComponentGrpcClient(serviceClass)
  }
  override def getGrpcClient[T](clientClass: Class[T], service: String): T = {
    getComponentGrpcClient(clientClass)
  }

  override def materializer(): Materializer = javaSdkContext.materializer()

  override def componentCallMetadata: MetadataImpl = {
    metadata.get(Telemetry.TRACE_PARENT_KEY) match {
      case Some(traceparent) =>
        MetadataImpl(
          kalix.javasdk.impl.MetadataImpl
            .of(List(MetadataEntry(Telemetry.TRACE_PARENT_KEY, MetadataEntry.Value.StringValue(traceparent)))))
      case None => MetadataImpl(kalix.javasdk.impl.MetadataImpl.Empty)
    }
  }

  override def getTracer: Tracer = javaSdkContext.getTracer

}
