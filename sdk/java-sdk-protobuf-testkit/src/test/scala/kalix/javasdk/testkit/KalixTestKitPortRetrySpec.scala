/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.testkit

import java.net.ServerSocket
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Supplier

import kalix.javasdk.Kalix
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class KalixTestKitPortRetrySpec extends AnyWordSpec with Matchers {

  // Helper: build a KalixTestKit with a custom port allocator injected via constructor.
  private def testKitWith(allocator: Supplier[Integer]): KalixTestKit =
    new KalixTestKit(new Kalix(), new Kalix().getMessageCodec, KalixTestKit.Settings.DEFAULT, allocator)

  // Mimics what availableLocalPort() does but for a specific port: binds to it and
  // returns the port number, or throws IllegalArgumentException if busy.
  private def allocateSpecificPort(port: Int): Int = {
    try {
      val s = new ServerSocket(port)
      s.close()
      port
    } catch {
      case _: Exception => throw new IllegalArgumentException(s"Port $port is already in use")
    }
  }

  "KalixTestKit.selectServicePort (used by start)" should {

    // ------------------------------------------------------------------
    // Phase 1 – demonstrates the problem: without retry the call fails
    // when the allocator cannot find a free port.
    // ------------------------------------------------------------------
    "throw when every call to the allocator fails" in {
      val busySocket = new ServerSocket(0)
      val busyPort = busySocket.getLocalPort

      // Allocator always tries to bind to the occupied port → always throws.
      val alwaysBusy: Supplier[Integer] = () => allocateSpecificPort(busyPort)

      val kit = testKitWith(alwaysBusy)

      // All maxRetries attempts fail → RuntimeException expected.
      intercept[RuntimeException] {
        kit.selectServicePort(true)
      }

      busySocket.close()
    }

    // ------------------------------------------------------------------
    // Phase 2 – verifies the retry logic: first attempt fails, second
    // uses availableLocalPort() and succeeds.
    // Before the retry loop was added this test failed; after adding the
    // loop it succeeds.
    // ------------------------------------------------------------------
    "succeed on retry when the first attempt fails but the second is free" in {
      val busySocket = new ServerSocket(0)
      val busyPort = busySocket.getLocalPort

      val callCount = new AtomicInteger(0)
      val busyThenFree: Supplier[Integer] = () => {
        if (callCount.incrementAndGet() == 1) allocateSpecificPort(busyPort)
        else KalixTestKit.availableLocalPort()
      }

      val kit = testKitWith(busyThenFree)

      val acquiredPort = kit.selectServicePort(true)

      acquiredPort should not be busyPort
      acquiredPort should be > 0

      // The allocator must have been called twice (once failing, once succeeding).
      callCount.get() shouldBe 2

      busySocket.close()
    }

    "return DEFAULT_USER_SERVICE_PORT when not using test containers" in {
      // allocator should never be called in non-container mode
      val neverCalled: Supplier[Integer] =
        () => fail("portAllocator should not be called when useTestContainers = false")

      val kit = testKitWith(neverCalled)

      kit.selectServicePort(false) shouldBe
      kalix.javasdk.testkit.impl.KalixRuntimeContainer.DEFAULT_USER_SERVICE_PORT
    }
  }
}
