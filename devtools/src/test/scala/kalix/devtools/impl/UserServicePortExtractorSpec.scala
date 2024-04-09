/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
