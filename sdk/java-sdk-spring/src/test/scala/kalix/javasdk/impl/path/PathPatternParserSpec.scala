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
