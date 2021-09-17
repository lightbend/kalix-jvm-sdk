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
import scala.compat.java8.FutureConverters
import akka.Done
import com.typesafe.config.Config

import com.akkaserverless.javasdk.{ AkkaServerless => Impl }

import com.akkaserverless.scalasdk.action.Action
import com.akkaserverless.scalasdk.action.ActionProvider
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntity
import com.akkaserverless.scalasdk.eventsourcedentity.EventSourcedEntityProvider
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedData
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntity
import com.akkaserverless.scalasdk.replicatedentity.ReplicatedEntityProvider
import com.akkaserverless.scalasdk.valueentity.ValueEntity
import com.akkaserverless.scalasdk.valueentity.ValueEntityProvider
import com.akkaserverless.scalasdk.view.View
import com.akkaserverless.scalasdk.view.ViewProvider

object AkkaServerless {
  def apply(impl: Impl) = new AkkaServerless(impl)
  def apply() = new AkkaServerless(new Impl())
}

/**
 * The AkkaServerless class is the main interface to configuring entities to deploy, and subsequently starting a local
 * server which will expose these entities to the AkkaServerless Proxy Sidecar.
 */
class AkkaServerless(impl: Impl) {

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
    AkkaServerless(impl.withClassLoader(classLoader))

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
    AkkaServerless(impl.withTypeUrlPrefix(prefix))

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the classpath, this specifies
   * that Java should be preferred.
   *
   * @return
   *   This AkkaServerless instance.
   */
  def preferJavaProtobufs: AkkaServerless =
    AkkaServerless(impl.preferJavaProtobufs)

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the classpath, this specifies
   * that Scala should be preferred.
   *
   * @return
   *   This AkkaServerless instance.
   */
  def preferScalaProtobufs: AkkaServerless =
    AkkaServerless(impl.preferScalaProtobufs)

  /**
   * Register a replicated entity using a {@link ReplicatedEntityProvider}. The concrete <code>
   * ReplicatedEntityProvider</code> is generated for the specific entities defined in Protobuf, for example
   * <code>CustomerEntityProvider</code>.
   *
   * <p>{@link ReplicatedEntityOptions} can be defined by in the <code>ReplicatedEntityProvider </code>.
   *
   * @return
   *   This stateful service builder.
   */
  def register[D <: ReplicatedData, E <: ReplicatedEntity[D]](
      provider: ReplicatedEntityProvider[D, E]): AkkaServerless =
    AkkaServerless(impl.register(provider.impl))

  /**
   * Register a value based entity using a {{@link ValueEntityProvider}}. The concrete <code> ValueEntityProvider</code>
   * is generated for the specific entities defined in Protobuf, for example <code>CustomerEntityProvider</code>.
   *
   * <p>{{@link ValueEntityOptions}} can be defined by in the <code>ValueEntityProvider</code>.
   *
   * @return
   *   This stateful service builder.
   */
  def register[S, E <: ValueEntity[S]](provider: ValueEntityProvider[S, E]): AkkaServerless =
    AkkaServerless(impl.register(provider.impl))

  /**
   * Register a event sourced entity using a {{@link EventSourcedEntityProvider}}. The concrete <code>
   * EventSourcedEntityProvider</code> is generated for the specific entities defined in Protobuf, for example
   * <code>CustomerEntityProvider</code>.
   *
   * <p>{{@link EventSourcedEntityOptions}} can be defined by in the <code> EventSourcedEntityProvider</code>.
   *
   * @return
   *   This stateful service builder.
   */
  def register[S, E <: EventSourcedEntity[S]](provider: EventSourcedEntityProvider[S, E]): AkkaServerless =
    AkkaServerless(impl.register(provider.impl))

  /**
   * Register a view using a {@link ViewProvider}. The concrete <code> ViewProvider</code> is generated for the specific
   * views defined in Protobuf, for example <code> CustomerViewProvider</code>.
   *
   * @return
   *   This stateful service builder.
   */
  def register[S, V <: View[S]](provider: ViewProvider[S, V]): AkkaServerless =
    AkkaServerless(impl.register(provider.impl))

  /**
   * Register an action using an {{@link ActionProvider}}. The concrete <code> ActionProvider</code> is generated for
   * the specific entities defined in Protobuf, for example <code>CustomerActionProvider</code>.
   *
   * @return
   *   This stateful service builder.
   */
  def register[A <: Action](provider: ActionProvider[A]): AkkaServerless =
    AkkaServerless(impl.register(provider.impl))

  /**
   * Starts a server with the configured entities.
   *
   * @return
   *   a CompletionStage which will be completed when the server has shut down.
   */
  def start(): Future[Done] = {
    createRunner().run
  }

  /**
   * Starts a server with the configured entities, using the supplied configuration.
   *
   * @return
   *   a CompletionStage which will be completed when the server has shut down.
   */
  def start(config: Config): Future[Done] = {
    createRunner(config).run
  }

  /**
   * Creates an AkkaServerlessRunner using the currently configured services. In order to start the server, `run()` must
   * be invoked on the returned AkkaServerlessRunner.
   *
   * @return
   *   an AkkaServerlessRunner
   */
  def createRunner(): AkkaServerlessRunner =
    new AkkaServerlessRunner(impl.createRunner())

  /**
   * Creates an AkkaServerlessRunner using the currently configured services, using the supplied configuration. In order
   * to start the server, `run()` must be invoked on the returned AkkaServerlessRunner.
   *
   * @return
   *   an AkkaServerlessRunner
   */
  def createRunner(config: Config): AkkaServerlessRunner =
    new AkkaServerlessRunner(impl.createRunner(config))
}
