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

package kalix.javasdk.impl.workflow

import akka.actor.testkit.typed.scaladsl.LoggingTestKit
import akka.actor.typed.scaladsl.adapter._
import akka.testkit.SocketUtil
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kalix.javasdk.Kalix
import kalix.javasdk.KalixRunner
import kalix.javasdk.workflow.WorkflowProvider

object TestWorkflow {

  def service(workflowProvider: WorkflowProvider[_, _]): TestWorkflow =
    new TestWorkflow(workflowProvider)

}

class TestWorkflow(workflowProvider: WorkflowProvider[_, _]) {

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
    .register(workflowProvider)
    .createRunner(config)

  runner.run()

  def expectLogError[T](message: String)(block: => T): T = {
    LoggingTestKit.error(message).expect(block)(runner.system.toTyped)
  }

  def terminate(): Unit = runner.terminate()

}
