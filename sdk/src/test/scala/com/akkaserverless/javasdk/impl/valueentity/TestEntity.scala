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

package com.akkaserverless.javasdk.impl.valueentity

import akka.testkit.EventFilter
import akka.testkit.SocketUtil
import com.akkaserverless.javasdk.{AkkaServerless, AkkaServerlessRunner}
import com.google.protobuf.Descriptors.{FileDescriptor, ServiceDescriptor}
import com.typesafe.config.{Config, ConfigFactory}
import scala.reflect.ClassTag

object TestEntity {
  def service[T: ClassTag](descriptor: ServiceDescriptor, fileDescriptors: FileDescriptor*): TestEntityService =
    new TestEntityService(implicitly[ClassTag[T]].runtimeClass, descriptor, fileDescriptors)
}

class TestEntityService(entityClass: Class[_], descriptor: ServiceDescriptor, fileDescriptors: Seq[FileDescriptor]) {
  val port: Int = SocketUtil.temporaryLocalPort()

  val config: Config = ConfigFactory.load(ConfigFactory.parseString(s"""
    akkaserverless {
      user-function-port = $port
      system.akka {
        loglevel = DEBUG
        loggers = ["akka.testkit.TestEventListener"]
        coordinated-shutdown.exit-jvm = off
      }
    }
  """))

  val runner: AkkaServerlessRunner = new AkkaServerless()
    .registerValueEntity(entityClass, descriptor, fileDescriptors: _*)
    .createRunner(config)

  runner.run()

  def expectLogError[T](message: String)(block: => T): T =
    EventFilter.error(message, occurrences = 1).intercept(block)(runner.system)

  def terminate(): Unit = runner.terminate()
}
