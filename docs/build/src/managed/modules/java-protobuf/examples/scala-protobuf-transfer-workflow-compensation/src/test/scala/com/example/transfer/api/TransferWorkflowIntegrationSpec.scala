package com.example.transfer.api

import java.util.UUID

import com.example.Main
import com.example.transfer.domain.TransferStatus.REQUIRES_MANUAL_INTERVENTION
import com.example.transfer.domain.TransferStatus.TRANSFER_ACCEPTATION_TIMED_OUT
import com.example.transfer.domain.TransferStatus.WAITING_FOR_ACCEPTATION
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

    "transfer funds with acceptation" in {
      val walletAId = randomId()
      val walletBId = randomId()
      val initWalletA = InitialBalance(walletAId, 2000)
      val initWalletB = InitialBalance(walletBId, 100)
      val transfer = Transfer(randomId(), walletAId, walletBId, 1001)
      walletClient.create(initWalletA).futureValue
      walletClient.create(initWalletB).futureValue

      transferClient.start(transfer).futureValue

      val transferState = transferClient.getTransferState(GetRequest(transfer.transferId)).futureValue
      transferState.status shouldBe WAITING_FOR_ACCEPTATION

      transferClient.accept(AcceptRequest(transfer.transferId)).futureValue

      eventually {
        val walletA = walletClient.getWalletState(api.GetRequest(walletAId)).futureValue
        val walletB = walletClient.getWalletState(api.GetRequest(walletBId)).futureValue
        walletA.balance shouldBe 999
        walletB.balance shouldBe 1101
      }
    }

    "timeout transfer funds acceptation" in {
      val walletAId = randomId()
      val walletBId = randomId()
      val initWalletA = InitialBalance(walletAId, 2000)
      val initWalletB = InitialBalance(walletBId, 100)
      val transfer = Transfer(randomId(), walletAId, walletBId, 1001)
      walletClient.create(initWalletA).futureValue
      walletClient.create(initWalletB).futureValue

      transferClient.start(transfer).futureValue

      transferClient.acceptationTimeout(AcceptationTimeoutRequest(transfer.transferId)).futureValue

      val transferState = transferClient.getTransferState(GetRequest(transfer.transferId)).futureValue
      transferState.status shouldBe TRANSFER_ACCEPTATION_TIMED_OUT

      val walletA = walletClient.getWalletState(api.GetRequest(walletAId)).futureValue
      val walletB = walletClient.getWalletState(api.GetRequest(walletBId)).futureValue
      walletA.balance shouldBe 2000
      walletB.balance shouldBe 100
    }

    "compensate failed transfer to not existing wallet" in {
      val walletAId = randomId()
      val walletBId = randomId()
      val initWalletA = InitialBalance(walletAId, 100)
      val transfer = Transfer(randomId(), walletAId, walletBId, 10)
      walletClient.create(initWalletA).futureValue

      transferClient.start(transfer).futureValue

      eventually {
        val walletA = walletClient.getWalletState(api.GetRequest(walletAId)).futureValue
        walletA.balance shouldBe 100
      }
    }

    "timed out transfer workflow" in {
      val transfer = Transfer(randomId(), randomId(), randomId(), 10)

      transferClient.start(transfer).futureValue

      eventually {
        val transferState = transferClient.getTransferState(GetRequest(transfer.transferId)).futureValue
        transferState.status shouldBe REQUIRES_MANUAL_INTERVENTION
      }
    }

  }

  override def afterAll(): Unit = {
    testKit.stop()
    super.afterAll()
  }

  private def randomId() = UUID.randomUUID.toString.substring(0, 8)
}
