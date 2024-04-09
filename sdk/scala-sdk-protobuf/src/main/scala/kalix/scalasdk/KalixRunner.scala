/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
