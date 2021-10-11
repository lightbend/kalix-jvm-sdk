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

package com.akkaserverless.javasdk.impl

import akka.Done
import akka.actor.ClassicActorSystemProvider
import akka.actor.CoordinatedShutdown
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.grpc.GrpcClientSettings
import akka.grpc.javadsl.{ AkkaGrpcClient => AkkaGrpcJavaClient }
import akka.grpc.scaladsl.{ AkkaGrpcClient => AkkaGrpcScalaClient }
import org.slf4j.LoggerFactory

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters._
import scala.jdk.FutureConverters._

/**
 * INTERNAL API
 */
object GrpcClients extends ExtensionId[GrpcClients] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): GrpcClients =
    new GrpcClients(system)
  override def lookup: ExtensionId[_ <: Extension] = this

  final private case class Key(serviceClass: Class[_], service: String)
}

/**
 * INTERNAL API
 */
final class GrpcClients(system: ExtendedActorSystem) extends Extension {
  import GrpcClients._
  private val log = LoggerFactory.getLogger(classOf[GrpcClients])

  private implicit val ec: ExecutionContext = system.dispatcher
  private val clients = new ConcurrentHashMap[Key, AnyRef]()

  CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceStop, "stop-grpc-clients")(() =>
    Future
      .traverse(clients.values().asScala) {
        case javaClient: AkkaGrpcJavaClient   => javaClient.close().asScala
        case scalaClient: AkkaGrpcScalaClient => scalaClient.close()
      }
      .map(_ => Done))

  def getGrpcClient[T](serviceClass: Class[T], service: String): T =
    clients.computeIfAbsent(Key(serviceClass, service), createClient(_)).asInstanceOf[T]

  private def createClient(key: Key): AnyRef = {
    val settings = if (!system.settings.config.hasPath(s"""akka.grpc.client."${key.service}"""")) {
      // "service" is not present in the config, treat it as an Akka gRPC inter-service call
      log.debug("Creating gRPC client for Akka Serverless service [{}]", key.service)
      GrpcClientSettings
        .connectToServiceAt(key.service, 80)(system)
        // (TLS is handled for us by Akka Serverless infra)
        .withTls(false)
    } else {
      log.debug("Creating gRPC client for external service [{}]", key.service)
      // external service, defined in config
      GrpcClientSettings.fromConfig(key.service)(system)
    }

    // expected to have a ServiceNameClient generated in the same package, so look that up through reflection
    val clientClass = system.dynamicAccess.getClassFor[AnyRef](key.serviceClass.getName + "Client").get
    val client =
      if (classOf[AkkaGrpcJavaClient].isAssignableFrom(clientClass)) {
        // Java API - static create
        val create = clientClass.getMethod("create", classOf[GrpcClientSettings], classOf[ClassicActorSystemProvider])
        create.invoke(null, settings, system)
      } else if (classOf[AkkaGrpcScalaClient].isAssignableFrom(clientClass)) {
        // Scala API - companion object apply
        val companion = system.dynamicAccess.getObjectFor[AnyRef](key.serviceClass.getName + "Client").get
        val create =
          companion.getClass.getMethod("apply", classOf[GrpcClientSettings], classOf[ClassicActorSystemProvider])
        create.invoke(companion, settings, system)
      }

    val closeDone = client match {
      case javaClient: AkkaGrpcJavaClient =>
        javaClient.closed().asScala
      case scalaClient: AkkaGrpcScalaClient => scalaClient.close()
    }
    closeDone.foreach { _ =>
      // if the client is closed, remove it from the pool
      log.debug("gRPC client for service [{}] was closed", key.service)
      clients.remove(key)
    }

    client
  }

}
