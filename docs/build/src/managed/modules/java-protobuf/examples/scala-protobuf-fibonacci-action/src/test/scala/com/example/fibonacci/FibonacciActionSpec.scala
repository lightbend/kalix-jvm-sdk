package com.example.fibonacci

import akka.actor.ActorSystem
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import kalix.scalasdk.action.Action
import kalix.scalasdk.testkit.ActionResult
import com.example.fibonacci
import org.scalatest.Assertions.pending
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

// tag::class[]
class FibonacciActionSpec
  extends AnyWordSpec
    with ScalaFutures
    with Matchers {

  "FibonacciAction" must {

    "handle command NextNumber" in {
      val testKit = FibonacciActionTestKit(new FibonacciAction(_)) // <1>
      val result = testKit.nextNumber(Number(5)) // <2>
      result.reply shouldBe (Number(8)) // <3>
    }
    // end::class[]

    "handle command NextNumbers" in {
      implicit val system = ActorSystem("fibonacci1") // needed to run stream
      try {
        val testKit = FibonacciActionTestKit(new FibonacciAction(_))
        val resultStream = testKit.nextNumbers(Number(5))
        val Seq(result1, result2) = resultStream.take(2).runWith(Sink.seq).futureValue
        result1.reply shouldBe Number(8)
        result2.reply shouldBe Number(13)
      } finally {
        system.terminate()
      }
    }

    // Not possible until testkit provides materializer https://github.com/lightbend/kalix-jvm-sdk/issues/495
    "handle command NextNumberOfSum" in pendingUntilFixed {
      val testKit = FibonacciActionTestKit(new FibonacciAction(_))
      val result = testKit.nextNumberOfSum(Source.single(Number(5)))
      result.reply shouldBe Number(8)
    }

    "handle command NextNumberOfEach" in {
      implicit val system = ActorSystem("fibonacci2") // needed to run stream
      try {
        val testKit = FibonacciActionTestKit(new FibonacciAction(_))
        val resultStream = testKit.nextNumberOfEach(Source(Number(5) :: Number(8) :: Nil))
        val Seq(result1, result2) = resultStream.take(2).runWith(Sink.seq).futureValue
        result1.reply shouldBe Number(8)
        result2.reply shouldBe Number(13)
      } finally {
        system.terminate()
      }
    }
    // tag::class[]
  }
}
// end::class[]
