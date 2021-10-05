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

package com.akkaserverless.scalasdk

import scala.concurrent.Future

import akka.Done
import com.akkaserverless.javasdk
import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.action.ActionProvider
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityProvider
import com.akkaserverless.scalasdk.impl.eventsourcedentity.JavaEventSourcedEntityProviderAdapter
import com.akkaserverless.scalasdk.impl.valueentity.JavaValueEntityProviderAdapter
import com.akkaserverless.scalasdk.impl.action.JavaActionProviderAdapter
import com.akkaserverless.scalasdk.impl.view.JavaViewProviderAdapter
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedData
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityProvider
import com.akkaserverless.scalasdk.valueentity.ValueEntity
import com.akkaserverless.scalasdk.valueentity.ValueEntityProvider
import com.akkaserverless.scalasdk.view.View
import com.akkaserverless.scalasdk.view.ViewProvider
import com.typesafe.config.Config
object AkkaServerless {
  def apply() = new AkkaServerless(new javasdk.AkkaServerless())

  private[scalasdk] def apply(impl: javasdk.AkkaServerless) =
    new AkkaServerless(impl.preferScalaProtobufs())
}

/**
 * The AkkaServerless class is the main interface to configuring entities to deploy, and subsequently starting a local
 * server which will expose these entities to the AkkaServerless Proxy Sidecar.
 */
class AkkaServerless private (private[akkaserverless] val delegate: javasdk.AkkaServerless) {

  /**
   * Sets the ClassLoader to be used for reflective access, the default value is the ClassLoader of the AkkaServerless
   * class.
   *
   * @param classLoader
   *   A non-null ClassLoader to be used for reflective access.
   * @return
   *   This AkkaServerless instance.
   */
  def withClassLoader(classLoader: ClassLoader): AkkaServerless =
    AkkaServerless(delegate.withClassLoader(classLoader))

  /**
   * Sets the type URL prefix to be used when serializing and deserializing types from and to Protobyf Any values.
   * Defaults to "type.googleapis.com".
   *
   * @param prefix
   *   the type URL prefix to be used.
   * @return
   *   This AkkaServerless instance.
   */
  def withTypeUrlPrefix(prefix: String): AkkaServerless =
    AkkaServerless(delegate.withTypeUrlPrefix(prefix))

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the classpath, this specifies
   * that Java should be preferred.
   *
   * @return
   *   This AkkaServerless instance.
   */
  def preferJavaProtobufs: AkkaServerless =
    AkkaServerless(delegate.preferJavaProtobufs)

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the classpath, this specifies
   * that Scala should be preferred.
   *
   * @return
   *   This AkkaServerless instance.
   */
  def preferScalaProtobufs: AkkaServerless =
    AkkaServerless(delegate.preferScalaProtobufs)

  /**
   * Register a replicated entity using a [[ReplicatedEntityProvider]]. The concrete `ReplicatedEntityProvider` is
   * generated for the specific entities defined in Protobuf, for example `CustomerEntityProvider`.
   *
   * [[com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityOptions]] can be defined by in the
   * `ReplicatedEntityProvider `.
   *
   * @return
   *   This stateful service builder.
   */
  def register[D <: ReplicatedData, E <: ReplicatedEntity[D]](
      provider: ReplicatedEntityProvider[D, E]): AkkaServerless =
    AkkaServerless(delegate.register(provider.impl))

  /**
   * Register a value based entity using a [[ValueEntityProvider]]. The concrete ` ValueEntityProvider` is generated for
   * the specific entities defined in Protobuf, for example `CustomerEntityProvider`.
   *
   * [[com.akkaserverless.scalasdk.valueentity.ValueEntityOptions]] can be defined by in the `ValueEntityProvider`.
   *
   * @return
   *   This stateful service builder.
   */
  def register[S, E <: ValueEntity[S]](provider: ValueEntityProvider[S, E]): AkkaServerless =
    AkkaServerless(delegate.register(new JavaValueEntityProviderAdapter(provider)))

  /**
   * Register a event sourced entity using a [[EventSourcedEntityProvider]]. The concrete `EventSourcedEntityProvider`
   * is generated for the specific entities defined in Protobuf, for example `CustomerEntityProvider`.
   *
   * [[com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityOptions]] can be defined by in the `
   * EventSourcedEntityProvider`.
   *
   * @return
   *   This stateful service builder.
   */
  def register[S, E <: EventSourcedEntity[S]](provider: EventSourcedEntityProvider[S, E]): AkkaServerless =
    AkkaServerless(delegate.register(new JavaEventSourcedEntityProviderAdapter(provider)))

  /**
   * Register a view using a [[ViewProvider]]. The concrete ` ViewProvider` is generated for the specific views defined
   * in Protobuf, for example ` CustomerViewProvider`.
   *
   * @return
   *   This stateful service builder.
   */
  def register[S, V <: View[S]](provider: ViewProvider[S, V]): AkkaServerless =
    AkkaServerless(delegate.register(new JavaViewProviderAdapter(provider)))

  /**
   * Register an action using an [[ActionProvider]]. The concrete ` ActionProvider` is generated for the specific
   * entities defined in Protobuf, for example `CustomerActionProvider`.
   *
   * @return
   *   This stateful service builder.
   */
  def register[A <: Action](provider: ActionProvider[A]): AkkaServerless =
    AkkaServerless(delegate.register(JavaActionProviderAdapter(provider)))

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
   * Creates an AkkaServerlessRunner using the currently configured services. In order to start the server, `run()` must
   * be invoked on the returned AkkaServerlessRunner.
   *
   * @return
   *   an AkkaServerlessRunner
   */
  def createRunner(): AkkaServerlessRunner =
    AkkaServerlessRunner(delegate.createRunner())

  /**
   * Creates an AkkaServerlessRunner using the currently configured services, using the supplied configuration. In order
   * to start the server, `run()` must be invoked on the returned AkkaServerlessRunner.
   *
   * @return
   *   an AkkaServerlessRunner
   */
  def createRunner(config: Config): AkkaServerlessRunner =
    AkkaServerlessRunner(delegate.createRunner(config))
}
