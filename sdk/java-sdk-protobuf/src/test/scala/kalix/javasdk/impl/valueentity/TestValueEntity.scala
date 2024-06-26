/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.valueentity

import akka.actor.testkit.typed.scaladsl.LoggingTestKit
import akka.actor.typed.scaladsl.adapter._
import akka.testkit.SocketUtil
import kalix.javasdk.{ Kalix, KalixRunner }
import kalix.javasdk.valueentity.ValueEntityProvider
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kalix.javasdk.impl.ProxyInfoHolder
import kalix.javasdk.impl.telemetry.TraceInstrumentation
import org.slf4j.event.Level

object TestValueEntity {
  def service(entityProvider: ValueEntityProvider[_, _], extraConfig: Option[Config] = None): TestValueService =
    new TestValueService(entityProvider, extraConfig)
}

class TestValueService(entityProvider: ValueEntityProvider[_, _], extraConfig: Option[Config] = None) {
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

  def expectLogError[T](message: String)(block: => T): T = {
    LoggingTestKit.error(message).expect(block)(runner.system.toTyped)
  }

  def expectLogInfoRegEx[T](regEx: String)(block: => T): T = {
    LoggingTestKit.empty.withLogLevel(Level.INFO).withMessageRegex(regEx).expect(block)(runner.system.toTyped)
  }

  def expectLogMdc[T](mdc: Map[String, String])(block: => T): T =
    LoggingTestKit.empty.withMdc(mdc).expect(block)(runner.system.toTyped)

  def terminate(): Unit = runner.terminate()
}
