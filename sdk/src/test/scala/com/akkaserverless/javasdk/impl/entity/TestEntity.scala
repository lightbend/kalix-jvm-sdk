/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl.entity

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
    .registerEntity(entityClass, descriptor, fileDescriptors: _*)
    .createRunner(config)

  runner.run()

  def expectLogError[T](message: String)(block: => T): T =
    EventFilter.error(message, occurrences = 1).intercept(block)(runner.system)

  def terminate(): Unit = runner.terminate()
}
