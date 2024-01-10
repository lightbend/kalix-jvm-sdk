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

import scala.annotation.nowarn
import scala.util.parsing.combinator.JavaTokenParsers
import scala.util.parsing.input.CharSequenceReader
import scala.util.parsing.input.Positional

import kalix.javasdk.impl.path.PathPatternParser.Literal
import kalix.javasdk.impl.path.PathPatternParser.Segment

object PathPatternParser extends JavaTokenParsers {

  @nowarn("msg=match may not be exhaustive") // for NoSuccess unapply
  final def parse(path: String): PathPattern =
    template(new CharSequenceReader(path)) match {
      case Success(template, _) =>
        val pattern = new PathPattern(path, template)
        pattern.validate()
        pattern
      case NoSuccess(msg, next) =>
        throw PathPatternParseException(msg, path, next.pos.column)
    }

  final case class Template(segments: List[Segment])
  final case class Segment(parts: List[SegmentPart]) extends Positional
  sealed trait SegmentPart
  final case class Literal(value: String) extends SegmentPart
  final case class DynamicChar() extends SegmentPart with Positional
  final case class Wildcard(rest: Boolean) extends SegmentPart with Positional
  final case class CapturingPart(name: String, regex: Option[String], rest: Boolean) extends SegmentPart with Positional

  private final val notLiteral = Set('*', '{', '}', '/', '?')
  private final val literal: Parser[String] = rep1(acceptIf(ch => !notLiteral(ch))(_ => "literal part")) ^^ (_.mkString)
  private final val literalPart: Parser[Literal] = literal ^^ Literal
  // Note that this will fail to parse regexes that contain a }
  private final val capturingPart: Parser[CapturingPart] =
    positioned(
      '{' ~> commit(
        '*'.? ~ ident ~ (':' ~> "[^}]*".r).? <~ '}'.withFailureMessage("Unclosed variable or unexpected character") ^^ {
          case rest ~ name ~ maybeRegex => CapturingPart(name, maybeRegex, rest.isDefined)
        }))
  private final val dynamicChar = positioned('?' ^^ (_ => DynamicChar()))
  private final val wildcard = positioned('*' ~> '*'.? ^^ (rest => Wildcard(rest.isDefined)))

  private final val segment: Parser[Segment] = positioned(
    rep1(literalPart | capturingPart | wildcard | dynamicChar) ^^ Segment)

  private final val segments: Parser[List[Segment]] = rep1(segment, '/' ~> segment)
  private final val endOfInput: Parser[None.type] = Parser { in =>
    if (!in.atEnd) {
      Error("Expected '/', path literal character, or end of input", in)
    } else {
      Success(None, in)
    }
  }

  private final val template: Parser[Template] = '/'.withFailureMessage("Template must start with a slash") ~>
    segments <~ endOfInput ^^ Template
}

object PathPattern {
  def apply(path: String): PathPattern = {
    val segment = Segment(List(Literal(path)))
    new PathPattern(path, PathPatternParser.Template(List(segment)))
  }
}
final class PathPattern(val path: String, template: PathPatternParser.Template) {
  import PathPatternParser._

  def validate(): Unit = {
    // gRPC HTTP transcoding doesn't support exactly the same mapping rules as Spring, including:
    // - No regexes
    // - No single dynamic characters
    // - All dynamic matching parts must be a full segment
    // todo ensure exceptions thrown here get handled such that the positional information associated with the exception is used
    template.segments.zipWithIndex.foreach { case (segment, idx) =>
      val last = idx == template.segments.size - 1
      // First apply validation rules to the parts
      segment.parts.foreach {
        case dc @ DynamicChar() =>
          throw new PathPatternParseException("Single character matchers not supported by Kalix Java SDK", path, dc)
        case cp @ CapturingPart(name, regex, rest) =>
          if (rest && !last) {
            throw new PathPatternParseException(
              s"Variable $name is configured to match the rest of the path but is not at the end of the pattern.",
              path,
              cp)
          }
          if (regex.isDefined) {
            throw new RuntimeException("Regex matchers not supported by Kalix Java SDK")
          }
        case w @ Wildcard(true) if !last =>
          throw new PathPatternParseException(s"Rest of path matcher is not at the end of the pattern.", path, w)
        case _ =>
      }
      // Now validate that there is only zero or one parts
      if (segment.parts.size > 1) {
        throw new PathPatternParseException(
          "Kalix Java SDK only supports capturing or dynamically matching whole path segments, partial segment matching not allowed.",
          path,
          segment)
      }
    }
  }
  def toGrpcTranscodingPattern: String = {
    // This won't produce a valid transcoding pattern if validate hasn't been called
    template.segments
      .map { segment =>
        segment.parts
          .map {
            case Literal(l)                    => l
            case CapturingPart(name, _, false) => s"{$name}"
            case CapturingPart(name, _, true)  => s"{$name=**}"
            case Wildcard(false)               => "*"
            case Wildcard(true)                => "**"
            case DynamicChar()                 => "*"
          }
          .mkString("")
      }
      .mkString("/", "/", "")
  }

  val fields: List[String] = {
    template.segments.flatMap(_.parts.collect { case CapturingPart(name, _, _) =>
      name
    })
  }
}

final case class PathPatternParseException(msg: String, path: String, column: Int)
    extends RuntimeException(
      s"$msg at ${if (column >= path.length) "end of input" else s"character $column"} of '$path'") {

  def this(msg: String, path: String, elem: Positional) = this(msg, path, elem.pos.column)

  def prettyPrint: String = {
    val caret =
      if (column >= path.length) ""
      else "\n" + path.take(column - 1).map { case '\t' => '\t'; case _ => ' ' } + "^"

    s"$msg at ${if (column >= path.length) "end of input" else s"character $column"}:\n$path$caret\n"
  }
}
