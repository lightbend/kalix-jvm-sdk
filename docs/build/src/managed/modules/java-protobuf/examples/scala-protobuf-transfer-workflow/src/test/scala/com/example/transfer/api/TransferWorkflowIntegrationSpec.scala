package com.example.transfer.api

import com.example.Main
import com.example.wallet.api
import com.example.wallet.api.InitialBalance
import com.example.wallet.api.WalletService
import kalix.scalasdk.testkit.KalixTestKit
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.time.Millis
import org.scalatest.time.Seconds
import org.scalatest.time.Span
import org.scalatest.wordspec.AnyWordSpec

// This class was initially generated based on the .proto definition by Kalix tooling.
//
// As long as this file exists it will not be overwritten: you can maintain it yourself,
// or delete it so it is regenerated as needed.

class TransferWorkflowIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures
    with Eventually {

  implicit private val patience: PatienceConfig =
    PatienceConfig(Span(5, Seconds), Span(500, Millis))

  private val testKit = KalixTestKit(Main.createKalix()).start()

  private val walletClient = testKit.getGrpcClient(classOf[WalletService])
  private val transferClient = testKit.getGrpcClient(classOf[TransferWorkflowService])

  "TransferWorkflow" should {

    "transfer funds between wallets" in {
      val initWalletA = InitialBalance("a", 100)
      val initWalletB = InitialBalance("b", 100)
      walletClient.create(initWalletA).futureValue
      walletClient.create(initWalletB).futureValue

      transferClient.start(Transfer("1", "a", "b", 10)).futureValue

      eventually {
        val walletA = walletClient.getWalletState(api.GetRequest("a")).futureValue
        val walletB = walletClient.getWalletState(api.GetRequest("b")).futureValue
        walletA.balance shouldBe 90
        walletB.balance shouldBe 110
      }
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }
}
