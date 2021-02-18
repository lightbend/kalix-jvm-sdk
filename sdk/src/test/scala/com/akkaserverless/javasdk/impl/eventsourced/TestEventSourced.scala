/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.eventsourced

import akka.testkit.EventFilter
import com.akkaserverless.javasdk.{AkkaServerless, AkkaServerlessRunner}
import com.akkaserverless.testkit.Sockets
import com.google.protobuf.Descriptors.{FileDescriptor, ServiceDescriptor}
import com.typesafe.config.{Config, ConfigFactory}

import scala.reflect.ClassTag

object TestEventSourced {
  def service[T: ClassTag](descriptor: ServiceDescriptor, fileDescriptors: FileDescriptor*): TestEventSourcedService =
    new TestEventSourcedService(implicitly[ClassTag[T]].runtimeClass, descriptor, fileDescriptors)
}

class TestEventSourcedService(entityClass: Class[_],
                              descriptor: ServiceDescriptor,
                              fileDescriptors: Seq[FileDescriptor]) {
  val port: Int = Sockets.temporaryLocalPort()

  val config: Config = ConfigFactory.load(ConfigFactory.parseString(s"""
    akkaserverless.user-function-port = $port
    akka {
      loglevel = ERROR
      loggers = ["akka.testkit.TestEventListener"]
      http.server {
        preview.enable-http2 = on
        idle-timeout = infinite
      }
    }
  """))

  val runner: AkkaServerlessRunner = new AkkaServerless()
    .registerEventSourcedEntity(entityClass, descriptor, fileDescriptors: _*)
    .createRunner(config)

  runner.run()

  def expectLogError[T](message: String)(block: => T): T =
    EventFilter.error(message, occurrences = 1).intercept(block)(runner.system)

  def terminate(): Unit = runner.terminate()
}
