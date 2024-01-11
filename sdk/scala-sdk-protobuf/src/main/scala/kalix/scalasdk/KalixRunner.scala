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

package kalix.scalasdk

import scala.concurrent.Future
import scala.compat.java8.FutureConverters
import scala.compat.java8.FunctionConverters._
import scala.jdk.CollectionConverters._
import akka.Done
import akka.actor.ActorSystem
import com.typesafe.config.Config
import kalix.javasdk

private[scalasdk] object KalixRunner {

  /**
   * Creates a KalixRunner from the given services. Use the default config to create the internal ActorSystem.
   */
  def apply(services: Map[String, ActorSystem => javasdk.impl.Service]): KalixRunner =
    new KalixRunner(new javasdk.KalixRunner(toJava(services), ScalaSdkBuildInfo.name))

  /**
   * Creates a KalixRunner from the given services and config. The config should have the same structure as the
   * reference.conf, with `kalix` as the root section, and the configuration for the internal ActorSystem is in the
   * `kalix.system` section.
   */
  def apply(services: Map[String, ActorSystem => javasdk.impl.Service], config: Config): KalixRunner =
    new KalixRunner(new javasdk.KalixRunner(toJava(services), config, ScalaSdkBuildInfo.name))

  def apply(impl: javasdk.KalixRunner): KalixRunner =
    new KalixRunner(impl)

  private def toJava(services: Map[String, ActorSystem => javasdk.impl.Service]) = services.map { case (k, v) =>
    k -> v.asJava
  }.asJava
}

class KalixRunner private (impl: javasdk.KalixRunner) {
  def run(): Future[Done] = {
    FutureConverters.toScala(impl.run())
  }
  def terminate(): Future[Done] = {
    FutureConverters.toScala(impl.terminate().thenApply(_ => Done))
  }
}
