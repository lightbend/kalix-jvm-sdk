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
import akka.actor.ActorSystem
import akka.actor.CoordinatedShutdown
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.grpc.GrpcClientSettings
import akka.grpc.scaladsl.AkkaGrpcClient

import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

/**
 * INTERNAL API
 */
object GrpcClients extends ExtensionId[GrpcClients] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): GrpcClients =
    new GrpcClients(system)
  override def lookup: ExtensionId[_ <: Extension] = this

  final private case class Key(clientClass: Class[_ <: AkkaGrpcClient], service: String)
}

/**
 * INTERNAL API
 */
final class GrpcClients(system: ExtendedActorSystem) extends Extension {
  import GrpcClients._

  private implicit val ec: ExecutionContext = system.dispatcher
  private val clients = new ConcurrentHashMap[Key, AkkaGrpcClient]()

  CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceStop, "stop-grpc-clients")(() =>
    Future.traverse(clients.values().asScala)(_.close()).map(_ => Done))

  def getGrpcClient[T <: AkkaGrpcClient](clientClass: Class[T], service: String): T =
    clients.computeIfAbsent(Key(clientClass, service), createClient(_)).asInstanceOf[T]

  private def createClient(key: Key): AkkaGrpcClient = {
    val settings = if (!system.settings.config.hasPath(s"""akka.grpc.client."${key.service}"""")) {
      // "service" is not present in the config, treat it as an Akka gRPC inter-service call
      GrpcClientSettings
        .connectToServiceAt(key.service, 80)(system)
        // (TLS is handled for us by Akka Serverless infra)
        .withTls(false)
    } else {
      // external service, defined in config
      GrpcClientSettings.fromConfig(key.service)(system)
    }

    val create = key.clientClass.getMethod("create", classOf[GrpcClientSettings])
    create.invoke(null, settings).asInstanceOf[AkkaGrpcClient]
  }

}
