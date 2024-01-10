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

class UserServicePortExtractorSpec extends AnyWordSpec with Matchers {

  "UserFunctionPortExtractor" should {
    "extract port from line with different formatting's but starting with USER_SERVICE_PORT" in {
      val lines =
        // UF ports can come together with env vars
        "USER_SERVICE_PORT:${USER_SERVICE_PORT:-8080}" ::
        "USER_SERVICE_PORT:    ${USER_SERVICE_PORT:-8080}" ::
        "         USER_SERVICE_PORT:    ${USER_SERVICE_PORT:-8080}" ::
        // UF ports can be defined as integers
        "USER_SERVICE_PORT:8080" ::
        "USER_SERVICE_PORT:        8080" ::
        "   USER_SERVICE_PORT:        8080" ::
        // UF ports can be defined as strings
        """USER_SERVICE_PORT:"8080"""" ::
        """USER_SERVICE_PORT: "8080" """ ::
        Nil

      lines.foreach { case UserServicePortExtractor(port) => port shouldBe 8080 }
    }

    "extract port from line with different formatting's but starting with USER_FUNCTION_PORT (legacy)" in {
      val lines =
        // UF ports can come together with env vars
        "USER_FUNCTION_PORT:${USER_FUNCTION_PORT:-8080}" ::
        "USER_FUNCTION_PORT:    ${USER_FUNCTION_PORT:-8080}" ::
        "         USER_FUNCTION_PORT:    ${USER_FUNCTION_PORT:-8080}" ::
        // UF ports can be defined as integers
        "USER_FUNCTION_PORT:8080" ::
        "USER_FUNCTION_PORT:        8080" ::
        "   USER_FUNCTION_PORT:        8080" ::
        // UF ports can be defined as strings
        """USER_FUNCTION_PORT:"8080"""" ::
        """USER_FUNCTION_PORT: "8080" """ ::
        Nil

      lines.foreach { case UserServicePortExtractor(port) => port shouldBe 8080 }
    }

    "not extract port from line not starting with USER_FUNCTION_PORT" in {

      "SOME_OTHER_PORT:8000" match {
        case UserServicePortExtractor(port) => fail("Should not match line not starting with USER_FUNCTION_PORT")
        case _                              => succeed
      }

    }
  }

}
