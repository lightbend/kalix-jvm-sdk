package com.example.wallet.api

import com.example.wallet.api
import com.example.wallet.domain.WalletState
import com.google.protobuf.empty.Empty
import kalix.scalasdk.testkit.ValueEntityResult
import kalix.scalasdk.valueentity.ValueEntity
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class WalletEntitySpec
    extends AnyWordSpec
    with Matchers {

  "WalletEntity" must {

    "have example test that can be removed" in {
      val service = WalletEntityTestKit(new WalletEntity(_))
      pending
      // use the testkit to execute a command
      // and verify final updated state:
      // val result = service.someOperation(SomeRequest)
      // verify the reply
      // val reply = result.getReply()
      // reply shouldBe expectedReply
      // verify the final state after the command
      // service.currentState() shouldBe expectedState
    }

    "handle command Create" in {
      val service = WalletEntityTestKit(new WalletEntity(_))
      pending
      // val result = service.create(InitialBalance(...))
    }

    "handle command Withdraw" in {
      val service = WalletEntityTestKit(new WalletEntity(_))
      pending
      // val result = service.withdraw(WithdrawRequest(...))
    }

    "handle command Deposit" in {
      val service = WalletEntityTestKit(new WalletEntity(_))
      pending
      // val result = service.deposit(DepositRequest(...))
    }

    "handle command GetWalletState" in {
      val service = WalletEntityTestKit(new WalletEntity(_))
      pending
      // val result = service.getWalletState(GetRequest(...))
    }

  }
}
