/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl.path

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PathPatternParserSpec extends AnyWordSpec with Matchers {

  "The Spring Path Pattern Parser" should {
    "parse a literal path" in {
      PathPatternParser.parse("/foo/bar").toGrpcTranscodingPattern should ===("/foo/bar")
    }
    "parse a path with a parameter" in {
      PathPatternParser.parse("/foo/{param}").toGrpcTranscodingPattern should ===("/foo/{param}")
    }

    "parse a path with multiple parameters" in {
      PathPatternParser.parse("/foo/{param1}/bar/{param2}").toGrpcTranscodingPattern should ===(
        "/foo/{param1}/bar/{param2}")
    }

  }

}
