/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.grpc.GrpcClientSettings
import akka.grpc.javadsl.AkkaGrpcClient
import kalix.javasdk.actionspec.actionspec_api.ActionSpecService
import kalix.javasdk.actionspec.actionspec_api.ActionSpecServiceClient
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import java.util.concurrent.CompletionStage

import scala.annotation.nowarn
import scala.concurrent.Promise
import scala.jdk.FutureConverters.FutureOps

import akka.actor.ClassicActorSystemProvider

// dummy instead of depending on actual generated Akka gRPC client to keep it simple
trait PretendService {}
object PretendServiceClient {

  def create(settings: GrpcClientSettings, @nowarn systemProvider: ClassicActorSystemProvider): PretendServiceClient =
    new PretendServiceClient(settings)
}
class PretendServiceClient(val settings: GrpcClientSettings) extends PretendService with AkkaGrpcClient {
  private val closePromise = Promise[Done]()
  def close(): CompletionStage[Done] = {
    closePromise.success(Done)
    closed()
  }

  def closed(): CompletionStage[Done] = closePromise.future.asJava
}

object GrpcClientsSpec {
  def config = ConfigFactory.parseString("""
     |akka.grpc.client.c {
     |  service-discovery {
     |    service-name = "my-service"
     |  }
     |  host = "my-host"
     |  port = 42
     |  override-authority = "google.fr"
     |  deadline = 10m
     |  user-agent = "Akka-gRPC"
     |}
     |""".stripMargin)
}

class GrpcClientsSpec
    extends ScalaTestWithActorTestKit(GrpcClientsSpec.config)
    with AnyWordSpecLike
    with Matchers
    with ScalaFutures {

  "The GrpcClients extension" must {
    "create the client for a service and pool it" in {

      val client1ForA = GrpcClients(system).getGrpcClient(classOf[PretendService], "a")
      client1ForA shouldBe a[PretendServiceClient]
      // no entry in config, so should be project inter-service call
      client1ForA match {
        case client: PretendServiceClient =>
          client.settings.serviceName should ===("a")
          client.settings.defaultPort should ===(80)
          client.settings.useTls should ===(false)
      }

      val client2ForA = GrpcClients(system).getGrpcClient(classOf[PretendService], "a")
      client2ForA shouldBe theSameInstanceAs(client1ForA)

      // same service protocol but different service
      val client1ForB = GrpcClients(system).getGrpcClient(classOf[PretendService], "b")
      (client1ForB shouldNot be).theSameInstanceAs(client1ForA)

      // same service protocol external service
      val client1ForC = GrpcClients(system).getGrpcClient(classOf[PretendService], "c")
      (client1ForC shouldNot be).theSameInstanceAs(client1ForA)
      client1ForC match {
        case client: PretendServiceClient =>
          client.settings.serviceName should ===("my-host")
          client.settings.defaultPort should ===(42)
          client.settings.useTls should ===(true)
      }
    }

    "create an instance of an actual generated gRPC client" in {
      // this is actually an Akka gRPC scala client, so no coverage for an actual generated Java Akka gRPC client here
      val client = GrpcClients(system).getGrpcClient(classOf[ActionSpecService], "actual")
      client shouldBe a[ActionSpecServiceClient]
    }
  }

}
