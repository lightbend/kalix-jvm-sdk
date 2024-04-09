/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.replicatedentity

import akka.actor.testkit.typed.scaladsl.LoggingTestKit
import akka.actor.typed.scaladsl.adapter._
import akka.testkit.SocketUtil
import kalix.javasdk.{ Kalix, KalixRunner }
import kalix.javasdk.replicatedentity.ReplicatedEntityProvider
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object TestReplicatedEntity {
  def service(entityProvider: ReplicatedEntityProvider[_, _]): TestReplicatedService =
    new TestReplicatedService(entityProvider)
}

class TestReplicatedService(entityProvider: ReplicatedEntityProvider[_, _]) {
  val port: Int = SocketUtil.temporaryLocalPort()

  val config: Config = ConfigFactory.load(ConfigFactory.parseString(s"""
    kalix {
      user-function-port = $port
      system.akka {
        loglevel = DEBUG
        loggers = ["akka.testkit.TestEventListener"]
        coordinated-shutdown.exit-jvm = off
      }
    }
  """))

  val runner: KalixRunner = new Kalix()
    .register(entityProvider)
    .createRunner(config)

  runner.run()

  def expectLogError[T](message: String)(block: => T): T =
    LoggingTestKit.error(message).expect(block)(runner.system.toTyped)

  def terminate(): Unit = runner.terminate()
}
