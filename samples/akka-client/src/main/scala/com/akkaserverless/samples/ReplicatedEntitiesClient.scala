/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.samples

import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl.{Keep, Sink}
import akka.stream.KillSwitches
import com.example.replicatedentity.replicated_entity_example._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
 * Designed for use in the REPL, run sbt console and then `new com.akkaserverless.samples.ReplicatedEntitiesClient("localhost", 9000)``
 */
class ReplicatedEntitiesClient(hostname: String, port: Int, hostnameOverride: Option[String], sys: ActorSystem) {
  def this(hostname: String, port: Int, hostnameOverride: Option[String] = None) =
    this(hostname, port, hostnameOverride, ActorSystem())
  private implicit val system = sys

  val settings = {
    val s = GrpcClientSettings.connectToServiceAt(hostname, port).withTls(false)
    hostnameOverride.fold(s)(host => s.withChannelBuilderOverrides(_.overrideAuthority(host)))
  }
  println(s"Connecting to $hostname:$port")
  val service = ReplicatedEntityExampleClient(settings)

  def shutdown(): Unit = {
    await(service.close())
    await(system.terminate())
  }

  def await[T](future: Future[T]): T = Await.result(future, 10.seconds)

  def getGCounter(id: String) = await(service.getGCounter(Get(id))).value

  def incrementGCounter(id: String, value: Long) = await(service.incrementGCounter(UpdateCounter(id, value))).value

  def getPNCounter(id: String) = await(service.getPNCounter(Get(id))).value

  def updatePNCounter(id: String, value: Long) = await(service.updatePNCounter(UpdateCounter(id, value))).value

  def getGSet(id: String) = await(service.getGSet(Get(id))).items

  def mutateGSet(id: String, values: Seq[SomeValue]) = await(service.mutateGSet(MutateSet(add = values))).size

  def getORSet(id: String) = await(service.getORSet(Get(id))).items

  def mutateORSet(id: String, add: Seq[SomeValue] = Nil, remove: Seq[SomeValue] = Nil, clear: Boolean = false) =
    await(service.mutateORSet(MutateSet(key = id, add = add, remove = remove, clear = clear))).size

  def connect(id: String) =
    service.connect(User(id)).viaMat(KillSwitches.single)(Keep.right).to(Sink.ignore).run()

  def monitor(monitorId: String, id: String) =
    service
      .monitor(User(id))
      .viaMat(KillSwitches.single)(Keep.right)
      .to(
        Sink.foreach(
          status =>
            println(
              s"Monitor $monitorId saw user $id go " + (if (status.online) "online"
                                                        else "offline")
            )
        )
      )
      .run()
}
