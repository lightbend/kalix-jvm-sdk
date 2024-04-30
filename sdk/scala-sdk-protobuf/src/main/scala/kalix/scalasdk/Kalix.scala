/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.scalasdk

import scala.concurrent.Future

import akka.Done
import kalix.javasdk
import kalix.replicatedentity.ReplicatedData
import kalix.scalasdk.action.Action
import kalix.scalasdk.action.ActionProvider
import kalix.scalasdk.eventsourcedentity.EventSourcedEntity
import kalix.scalasdk.eventsourcedentity.EventSourcedEntityProvider
import kalix.scalasdk.impl.eventsourcedentity.JavaEventSourcedEntityProviderAdapter
import kalix.scalasdk.impl.valueentity.JavaValueEntityProviderAdapter
import kalix.scalasdk.impl.action.JavaActionProviderAdapter
import kalix.scalasdk.impl.replicatedentity.JavaReplicatedEntityProviderAdapter
import kalix.scalasdk.impl.view.JavaViewProviderAdapter
import kalix.scalasdk.replicatedentity.ReplicatedEntity
import kalix.scalasdk.replicatedentity.ReplicatedEntityProvider
import kalix.scalasdk.valueentity.ValueEntity
import kalix.scalasdk.valueentity.ValueEntityProvider
import kalix.scalasdk.view.ViewProvider
import com.typesafe.config.Config
import kalix.javasdk.impl.MessageCodec
import kalix.scalasdk.impl.workflow.JavaWorkflowProviderAdapter
import kalix.scalasdk.workflow.AbstractWorkflow
import kalix.scalasdk.workflow.WorkflowProvider

object Kalix {
  def apply() = new Kalix(new javasdk.Kalix().preferScalaProtobufs().withSdkName(ScalaSdkBuildInfo.name))

  private[scalasdk] def apply(impl: javasdk.Kalix) =
    new Kalix(impl)
}

/**
 * The Kalix class is the main interface to configuring entities to deploy, and subsequently starting a local server
 * which will expose these entities to the Kalix Runtime Sidecar.
 */
class Kalix private (private[kalix] val delegate: javasdk.Kalix) {

  /**
   * Sets the ClassLoader to be used for reflective access, the default value is the ClassLoader of the Kalix class.
   *
   * @param classLoader
   *   A non-null ClassLoader to be used for reflective access.
   * @return
   *   This Kalix instance.
   */
  def withClassLoader(classLoader: ClassLoader): Kalix =
    Kalix(delegate.withClassLoader(classLoader))

  /**
   * Sets the type URL prefix to be used when serializing and deserializing types from and to Protobyf Any values.
   * Defaults to "type.googleapis.com".
   *
   * @param prefix
   *   the type URL prefix to be used.
   * @return
   *   This Kalix instance.
   */
  def withTypeUrlPrefix(prefix: String): Kalix =
    Kalix(delegate.withTypeUrlPrefix(prefix))

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the classpath, this specifies
   * that Java should be preferred.
   *
   * @return
   *   This Kalix instance.
   */
  def preferJavaProtobufs: Kalix =
    Kalix(delegate.preferJavaProtobufs)

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the classpath, this specifies
   * that Scala should be preferred.
   *
   * @return
   *   This Kalix instance.
   */
  def preferScalaProtobufs: Kalix =
    Kalix(delegate.preferScalaProtobufs)

  /**
   * Register a replicated entity using a [[kalix.scalasdk.replicatedentity.ReplicatedEntityProvider]]. The concrete
   * `ReplicatedEntityProvider` is generated for the specific entities defined in Protobuf, for example
   * `CustomerEntityProvider`.
   *
   * [[kalix.scalasdk.replicatedentity.ReplicatedEntityOptions]] can be defined by in the `ReplicatedEntityProvider `.
   *
   * @return
   *   This stateful service builder.
   */
  def register[D <: ReplicatedData, E <: ReplicatedEntity[D]](provider: ReplicatedEntityProvider[D, E]): Kalix =
    Kalix(delegate.register(JavaReplicatedEntityProviderAdapter(provider)))

  /**
   * Register a value based entity using a [[kalix.scalasdk.valueentity.ValueEntityProvider]]. The concrete `
   * ValueEntityProvider` is generated for the specific entities defined in Protobuf, for example
   * `CustomerEntityProvider`.
   *
   * [[kalix.scalasdk.valueentity.ValueEntityOptions]] can be defined by in the `ValueEntityProvider`.
   *
   * @return
   *   This stateful service builder.
   */
  def register[S, E <: ValueEntity[S]](provider: ValueEntityProvider[S, E]): Kalix =
    Kalix(delegate.register(new JavaValueEntityProviderAdapter(provider)))

  /**
   * Register a workflow using a [[kalix.scalasdk.workflow.WorkflowProvider]]. The concrete `WorkflowProvider` is
   * generated for the specific workflow defined in Protobuf, for example `TransferWorkflowProvider`.
   *
   * [[kalix.scalasdk.workflow.WorkflowOptions]] can be defined by in the `WorkflowProvider`.
   *
   * @return
   *   This stateful service builder.
   */
  def register[S >: Null, E <: AbstractWorkflow[S]](provider: WorkflowProvider[S, E]): Kalix =
    Kalix(delegate.register(new JavaWorkflowProviderAdapter(provider)))

  /**
   * Register a event sourced entity using a [[kalix.scalasdk.eventsourcedentity.EventSourcedEntityProvider]]. The
   * concrete `EventSourcedEntityProvider` is generated for the specific entities defined in Protobuf, for example
   * `CustomerEntityProvider`.
   *
   * [[kalix.scalasdk.eventsourcedentity.EventSourcedEntityOptions]] can be defined by in the
   * `EventSourcedEntityProvider`.
   *
   * @return
   *   This stateful service builder.
   */
  def register[S, E <: EventSourcedEntity[S]](provider: EventSourcedEntityProvider[S, E]): Kalix =
    Kalix(delegate.register(new JavaEventSourcedEntityProviderAdapter(provider)))

  /**
   * Register a view using a [[kalix.scalasdk.view.ViewProvider]]. The concrete ` ViewProvider` is generated for the
   * specific views defined in Protobuf, for example ` CustomerViewProvider`.
   *
   * @return
   *   This stateful service builder.
   */
  def register(provider: ViewProvider): Kalix =
    Kalix(delegate.register(new JavaViewProviderAdapter(provider)))

  /**
   * Register an action using an [[kalix.scalasdk.action.ActionProvider]]. The concrete ` ActionProvider` is generated
   * for the specific entities defined in Protobuf, for example `CustomerActionProvider`.
   *
   * @return
   *   This stateful service builder.
   */
  def register[A <: Action](provider: ActionProvider[A]): Kalix =
    Kalix(delegate.register(JavaActionProviderAdapter(provider)))

  /**
   * Starts a server with the configured entities.
   *
   * @return
   *   a CompletionStage which will be completed when the server has shut down.
   */
  def start(): Future[Done] = {
    createRunner().run()
  }

  /**
   * Starts a server with the configured entities, using the supplied configuration.
   *
   * @return
   *   a CompletionStage which will be completed when the server has shut down.
   */
  def start(config: Config): Future[Done] = {
    createRunner(config).run()
  }

  /**
   * Creates a KalixRunner using the currently configured services. In order to start the server, `run()` must be
   * invoked on the returned KalixRunner.
   *
   * @return
   *   a KalixRunner
   */
  def createRunner(): KalixRunner =
    KalixRunner(delegate.createRunner())

  /**
   * Creates a KalixRunner using the currently configured services, using the supplied configuration. In order to start
   * the server, `run()` must be invoked on the returned KalixRunner.
   *
   * @return
   *   a KalixRunner
   */
  def createRunner(config: Config): KalixRunner =
    KalixRunner(delegate.createRunner(config))

  def getMessageCodec(): MessageCodec =
    delegate.getMessageCodec
}
