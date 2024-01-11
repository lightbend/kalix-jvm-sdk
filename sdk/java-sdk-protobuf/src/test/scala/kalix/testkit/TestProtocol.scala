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

package kalix.testkit

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.testkit.TestKit
import kalix.testkit.eventsourcedentity.TestEventSourcedProtocol
import kalix.testkit.replicatedentity.TestReplicatedEntityProtocol
import kalix.testkit.valueentity.TestValueEntityProtocol
import com.typesafe.config.{ Config, ConfigFactory }
import kalix.testkit.workflow.TestWorkflowProtocol

// FIXME: should we be doing protocol-level testing in the SDK?
// Copied over from Kalix framework (parts that are used here).
final class TestProtocol(host: String, port: Int) {
  import TestProtocol._

  val context = new TestProtocolContext(host, port)

  val eventSourced = new TestEventSourcedProtocol(context)
  val valueEntity = new TestValueEntityProtocol(context)
  val replicatedEntity = new TestReplicatedEntityProtocol(context)
  val workflow = new TestWorkflowProtocol(context)

  def settings: GrpcClientSettings = context.clientSettings

  def terminate(): Unit = {
    eventSourced.terminate()
    valueEntity.terminate()
    replicatedEntity.terminate()
    workflow.terminate()
  }
}

object TestProtocol {
  def apply(port: Int): TestProtocol = apply("localhost", port)
  def apply(host: String, port: Int): TestProtocol = new TestProtocol(host, port)

  final class TestProtocolContext(val host: String, val port: Int) {
    val config: Config = ConfigFactory.load(ConfigFactory.parseString(s"""
      akka.loglevel = ERROR
      akka.stdout-loglevel = ERROR
      akka.http.server {
        preview.enable-http2 = on
      }
    """))

    implicit val system: ActorSystem = ActorSystem("TestProtocol", config)

    val clientSettings: GrpcClientSettings = GrpcClientSettings.connectToServiceAt(host, port).withTls(false)

    def terminate(): Unit = TestKit.shutdownActorSystem(system)
  }
}
