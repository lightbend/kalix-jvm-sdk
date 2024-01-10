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

package kalix.devtools.impl

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class HostAndPortSpec extends AnyWordSpec with Matchers {

  "HostAndPort util" should {

    "extract host and port from 'somehost:9001'" in {
      val (host, port) = HostAndPort.extract("somehost:9001")
      host shouldBe "somehost"
      port shouldBe 9001
    }

    "extract host and port from 'some.host:9001'" in {
      val (host, port) = HostAndPort.extract("some.host:9001")
      host shouldBe "some.host"
      port shouldBe 9001
    }

    "extract port from '9001'" in {
      val (host, port) = HostAndPort.extract("9001")
      host shouldBe "0.0.0.0"
      port shouldBe 9001
    }

    "throw IllegalArgumentException if port is invalid" in {
      List("", "-1", "-81", "65536", "123456", "1234567890")
        .foreach { port =>
          intercept[IllegalArgumentException] {
            HostAndPort.extract(port)
          }
        }
    }

    "throw IllegalArgumentException if host:port is invalid" in {

      List(
        "localhost-5678", // dash
        "localhost:-5678", // negative port
        "localhost", // no port
        "foo.bar.com",
        "localhost|5678", // pipe
        "localhost:port", // not number
        "localhost;1234", // semi-colon
        "localhost:123456", // port too large
        "localhost:1234:4321", // double colon
        "localhost:65536").foreach { hostPort =>
        intercept[IllegalArgumentException] {
          HostAndPort.extract(hostPort)
        }
      }
    }
  }
}
