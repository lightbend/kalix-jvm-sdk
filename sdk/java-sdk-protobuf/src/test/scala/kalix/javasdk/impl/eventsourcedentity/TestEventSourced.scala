/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.eventsourcedentity

import akka.actor.testkit.typed.scaladsl.LoggingTestKit
import akka.actor.typed.scaladsl.adapter._
import akka.testkit.SocketUtil
import kalix.javasdk.{ Kalix, KalixRunner }
import com.typesafe.config.{ Config, ConfigFactory }
import kalix.javasdk.eventsourcedentity.EventSourcedEntityProvider
import kalix.javasdk.impl.telemetry.TraceInstrumentation
import kalix.javasdk.impl.ProxyInfoHolder

object TestEventSourced {
  def service(
      entityProvider: EventSourcedEntityProvider[_, _, _],
      extraConfig: Option[Config] = None): TestEventSourcedService =
    new TestEventSourcedService(entityProvider, extraConfig)
}

class TestEventSourcedService(entityProvider: EventSourcedEntityProvider[_, _, _], extraConfig: Option[Config] = None) {
  val port: Int = SocketUtil.temporaryLocalPort()

  val config: Config = ConfigFactory.load(
    ConfigFactory
      .parseString(s"""
    kalix {
      user-function-port = $port
      system.akka {
        loglevel = DEBUG
        coordinated-shutdown.exit-jvm = off
      }
    }
  """).withFallback(extraConfig.getOrElse(ConfigFactory.empty)))

  val runner: KalixRunner = new Kalix()
    .register(entityProvider)
    .createRunner(config)

  runner.run()
  //setting tracing as it was sent by the proxy and discovered by the user function
  if (config.hasPath(TraceInstrumentation.TRACING_ENDPOINT)) {
    ProxyInfoHolder(runner.system).overrideTracingCollectorEndpoint(
      config.getString(TraceInstrumentation.TRACING_ENDPOINT))
  } else {
    ProxyInfoHolder(runner.system).overrideTracingCollectorEndpoint("");
  }

  def expectLogError[T](message: String)(block: => T): T =
    LoggingTestKit.error(message).expect(block)(runner.system.toTyped)

  def expectLogMdc[T](mdc: Map[String, String])(block: => T): T =
    LoggingTestKit.empty.withMdc(mdc).expect(block)(runner.system.toTyped)

  def terminate(): Unit = runner.terminate()
}
