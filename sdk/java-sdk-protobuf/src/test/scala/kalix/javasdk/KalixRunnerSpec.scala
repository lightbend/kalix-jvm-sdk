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

package kalix.javasdk

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class KalixRunnerSpec extends AnyWordSpec with Matchers {

  "KalixRunner" should {
    "add local discovery config for 0.0.0.0:port in dev-mode" in {
      val config = ConfigFactory.parseString(s"""
           |kalix {
           |  system {}
           |  dev-mode.service-port-mappings {
           |    foo = "9001"
           |  }
           |}
           |""".stripMargin)

      val prepared = KalixRunner.prepareConfig(config)
      val clientConfig = prepared.getConfig("akka.grpc.client.foo")
      clientConfig.getString("host") shouldBe "0.0.0.0"
      clientConfig.getInt("port") shouldBe 9001
    }

    "add local discovery config for somehost:port in dev-mode" in {
      val config = ConfigFactory.parseString(s"""
           |kalix {
           |  system {}
           |  dev-mode.service-port-mappings {
           |    foo = "somehost:9001"
           |  }
           |}
           |""".stripMargin)

      val prepared = KalixRunner.prepareConfig(config)
      val clientConfig = prepared.getConfig("akka.grpc.client.foo")
      clientConfig.getString("host") shouldBe "somehost"
      clientConfig.getInt("port") shouldBe 9001
    }

    "add local discovery config for some.host:port in dev-mode" in {
      val config = ConfigFactory.parseString(s"""
           |kalix {
           |  system {}
           |  dev-mode.service-port-mappings {
           |    foo = "some.host:9001"
           |  }
           |}
           |""".stripMargin)

      val prepared = KalixRunner.prepareConfig(config)
      val clientConfig = prepared.getConfig("akka.grpc.client.foo")
      clientConfig.getString("host") shouldBe "some.host"
      clientConfig.getInt("port") shouldBe 9001
    }

    "fail config parsing if invalid host:port format" in {
      val config = ConfigFactory.parseString(s"""
           |kalix {
           |  system {}
           |  dev-mode.service-port-mappings {
           |    foo = "some.host-9001"
           |  }
           |}
           |""".stripMargin)

      intercept[IllegalArgumentException] {
        KalixRunner.prepareConfig(config)
      }.getMessage shouldBe "Invalid service port mapping: some.host-9001"
    }

    "fail config parsing if invalid config type" in {
      val config = ConfigFactory.parseString(s"""
           |kalix {
           |  system {}
           |  dev-mode.service-port-mappings {
           |    foo = 100
           |  }
           |}
           |""".stripMargin)

      intercept[IllegalArgumentException] {
        KalixRunner.prepareConfig(config)
      }.getMessage shouldBe "Invalid config type. Settings 'kalix.dev-mode.service-port-mappings.foo' should be of type String"
    }
  }
}
