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

import org.scalatest.OptionValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ServicePortMappingsExtractorSpec extends AnyWordSpec with Matchers with OptionValues {

  "ServicePortMappingsExtractor" should {

    "collect all occurrences of kalix.dev-mode.service-port-mappings" in {
      val line =
        "-Dsomething.else=10 " +
        "-Dkalix.dev-mode.service-port-mappings.foo=9001 " +
        "-Dsomething.in.the.middle=20 " +
        "-Dkalix.dev-mode.service-port-mappings.bar=9002 " +
        "-Dkalix.dev-mode.service-port-mappings.baz=9003" +
        "-Dkalix.dev-mode.service-port-mappings.qux=somehost:9004"

      ServicePortMappingsExtractor.unapply(line).value shouldBe Seq(
        "foo=9001",
        "bar=9002",
        "baz=9003",
        "qux=somehost:9004")
    }

    "collect nothing if there are no occurrences of kalix.dev-mode.service-port-mappings" in {
      val line =
        "-Dsomething.else=10 " +
        "-Dsomething.in.the.middle=20"

      ServicePortMappingsExtractor.unapply(line) shouldBe None
    }
  }
}
