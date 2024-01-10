/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk.impl

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory
import kalix.protocol.discovery.ProxyInfo
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DiscoverySpec extends AnyWordSpec with Matchers with ScalaFutures {

  "Discovery" should {

    "pass along env by default" in {
      var system: ActorSystem[Nothing] = null
      try {
        system = ActorSystem[Nothing](Behaviors.empty[Nothing], "DiscoverySpec1")
        val discovery = new DiscoveryImpl(system.classicSystem, Map.empty, None, "test")
        val result = discovery.discover(ProxyInfo()).futureValue
        result.getServiceInfo.env should not be empty
      } finally {
        system.terminate()
      }
    }

    "pass along only allowed names if configured" in {
      var system: ActorSystem[Nothing] = null
      try {
        system = ActorSystem[Nothing](
          Behaviors.empty[Nothing],
          "DiscoverySpec2",
          ConfigFactory.parseString("""kalix.discovery.pass-along-env-allow = ["HOME"]"""))
        val discovery = new DiscoveryImpl(system.classicSystem, Map.empty, None, "test")
        val result = discovery.discover(ProxyInfo()).futureValue
        result.getServiceInfo.env should have size 1
      } finally {
        system.terminate()
      }
    }

    "pass along nothing if no names allowed" in {
      var system: ActorSystem[Nothing] = null
      try {
        system = ActorSystem[Nothing](
          Behaviors.empty[Nothing],
          "DiscoverySpec2",
          ConfigFactory.parseString("""kalix.discovery.pass-along-env-allow = []"""))
        val discovery = new DiscoveryImpl(system.classicSystem, Map.empty, None, "test")
        val result = discovery.discover(ProxyInfo()).futureValue
        result.getServiceInfo.env should be(empty)
      } finally {
        system.terminate()
      }
    }

  }

}
