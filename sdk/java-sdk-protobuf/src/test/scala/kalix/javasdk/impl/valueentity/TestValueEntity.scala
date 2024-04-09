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

object TestValueEntity {
  def service(entityProvider: ValueEntityProvider[_, _]): TestValueService =
    new TestValueService(entityProvider)
}

class TestValueService(entityProvider: ValueEntityProvider[_, _]) {
  val port: Int = SocketUtil.temporaryLocalPort()

  val config: Config = ConfigFactory.load(ConfigFactory.parseString(s"""
    kalix {
      user-function-port = $port
      system.akka {
        loglevel = DEBUG
        coordinated-shutdown.exit-jvm = off
      }
    }
  """))

  val runner: KalixRunner = new Kalix()
    .register(entityProvider)
    .createRunner(config)

  runner.run()
  //setting tracing as disabled, emulating that is discovered from the proxy.
  ProxyInfoHolder(runner.system).overrideTracingCollectorEndpoint("")

  def expectLogError[T](message: String)(block: => T): T = {
    LoggingTestKit.error(message).expect(block)(runner.system.toTyped)
  }

  def terminate(): Unit = runner.terminate()
}
