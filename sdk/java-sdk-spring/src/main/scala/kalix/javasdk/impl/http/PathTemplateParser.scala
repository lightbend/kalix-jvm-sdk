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

package kalix.javasdk.impl.http

import java.util.regex.Pattern

import scala.annotation.nowarn
import scala.collection.mutable
import scala.util.matching.Regex
import scala.util.parsing.combinator.Parsers
import scala.util.parsing.input.CharSequenceReader
import scala.util.parsing.input.Positional

import kalix.javasdk.impl.path.PathPatternParseException

/**
 * INTERNAL API
 */
object PathTemplateParser extends Parsers {

  override type Elem = Char

  final class ParsedTemplate(val path: String, template: Template) {
    val regex: Regex = {
      def doToRegex(
          builder: mutable.StringBuilder,
          segments: List[Segment],
          matchSlash: Boolean): mutable.StringBuilder =
        segments match {
          case Nil => builder // Do nothing
          case head :: tail =>
            if (matchSlash) {
              builder.append('/')
            }

            head match {
              case LiteralSegment(literal) =>
                builder.append(Pattern.quote(literal))
              case SingleSegmentMatcher =>
                builder.append("[^/:]*")
              case MultiSegmentMatcher() =>
                builder.append(".*")
              case VariableSegment(_, None) =>
                builder.append("([^/:]*)")
              case VariableSegment(_, Some(template)) =>
                builder.append('(')
                doToRegex(builder, template, matchSlash = false)
                builder.append(')')
            }

            doToRegex(builder, tail, matchSlash = true)
        }

      val builder = doToRegex(new mutable.StringBuilder, template.segments, matchSlash = true)

      template.verb
        .foldLeft(builder) { (builder, verb) =>
          builder.append(':').append(Pattern.quote(verb))
        }
        .toString()
        .r
    }

    val fields: List[TemplateVariable] = {
      var found = Set.empty[List[String]]
      template.segments.collect {
        case v @ VariableSegment(fieldPath, _) if found(fieldPath) =>
          throw PathPatternParseException("Duplicate path in template", path, v.pos.column + 1)
        case VariableSegment(fieldPath, segments) =>
          found += fieldPath
          TemplateVariable(
            fieldPath,
            segments.exists(_ match {
              case ((_: MultiSegmentMatcher) :: _) | (_ :: _ :: _) => true
              case _                                               => false
            }))
      }
    }
  }

  final case class TemplateVariable(fieldPath: List[String], multi: Boolean)

  @nowarn("msg=match may not be exhaustive") // for NoSuccess unapply
  final def parse(path: String): ParsedTemplate =
    template(new CharSequenceReader(path)) match {
      case Success(template, _) =>
        new ParsedTemplate(path, validate(path, template))
      case NoSuccess(msg, next) =>
        throw PathPatternParseException(msg, path, next.pos.column)
    }

  private final def validate(path: String, template: Template): Template = {
    def flattenSegments(segments: Segments, allowVariables: Boolean): Segments =
      segments.flatMap {
        case variable: VariableSegment if !allowVariables =>
          throw PathPatternParseException("Variable segments may not be nested", path, variable.pos.column)
        case VariableSegment(_, Some(nested)) => flattenSegments(nested, false)
        case other                            => List(other)
      }

    // Flatten, verifying that there are no nested variables
    val flattened = flattenSegments(template.segments, true)

    // Verify there are no ** matchers that aren't the last matcher
    flattened.dropRight(1).foreach {
      case m @ MultiSegmentMatcher() =>
        throw PathPatternParseException(
          "Multi segment matchers (**) may only be in the last position of the template",
          path,
          m.pos.column)
      case _ =>
    }
    template
  }

  // AST for syntax described here:
  // https://cloud.google.com/endpoints/docs/grpc-service-config/reference/rpc/google.api#google.api.HttpRule.description.subsection
  // Note that there are additional rules (eg variables cannot contain nested variables) that this AST doesn't enforce,
  // these are validated elsewhere.
  private final case class Template(segments: Segments, verb: Option[Verb])
  private type Segments = List[Segment]
  private type Verb = String
  private sealed trait Segment
  private final case class LiteralSegment(literal: Literal) extends Segment
  private final case class VariableSegment(fieldPath: FieldPath, template: Option[Segments])
      extends Segment
      with Positional
  private type FieldPath = List[Ident]
  private case object SingleSegmentMatcher extends Segment
  private final case class MultiSegmentMatcher() extends Segment with Positional
  private type Literal = String
  private type Ident = String

  private final val NotLiteral = Set('*', '{', '}', '/', ':', '\n')

  // Matches ident syntax from https://developers.google.com/protocol-buffers/docs/reference/proto3-spec
  private final val ident: Parser[Ident] = rep1(
    acceptIf(ch => (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z'))(e =>
      s"Expected identifier first letter, but got '$e'"),
    acceptIf(ch => (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_')(_ =>
      "identifier part")) ^^ (_.mkString)

  // There is no description of this in the spec. It's not a URL segment, since the spec explicitly says that the value
  // must be URL encoded when expressed as a URL. Since all segments are delimited by a / character or a colon, and a
  // literal may only be a full segment, we could assume it's any non slash or colon character, but that would mean
  // syntax errors in variables for example would end up being parsed as literals, which wouldn't give nice error
  // messages at all. So we'll be a little more strict, and not allow *, { or } in any literals.
  private final val literal: Parser[Literal] = rep(acceptIf(ch => !NotLiteral(ch))(_ => "literal part")) ^^ (_.mkString)

  private final val fieldPath: Parser[FieldPath] = rep1(ident, '.' ~> ident)

  private final val literalSegment: Parser[LiteralSegment] = literal ^^ LiteralSegment
  // After we see an open curly, we commit to erroring if we fail to parse the remainder.
  private final def variable: Parser[VariableSegment] =
    positioned(
      '{' ~> commit(
        fieldPath ~ ('=' ~> segments).? <~ '}'.withFailureMessage("Unclosed variable or unexpected character") ^^ {
          case fieldPath ~ maybeTemplate => VariableSegment(fieldPath, maybeTemplate)
        }))
  private final val singleSegmentMatcher: Parser[SingleSegmentMatcher.type] = '*' ^^ (_ => SingleSegmentMatcher)
  private final val multiSegmentMatcher: Parser[MultiSegmentMatcher] = positioned(
    '*' ~ '*' ^^ (_ => MultiSegmentMatcher()))
  private final val segment: Parser[Segment] = commit(
    multiSegmentMatcher | singleSegmentMatcher | variable | literalSegment)

  private final val verb: Parser[Verb] = ':' ~> literal
  private final val segments: Parser[Segments] = rep1(segment, '/' ~> segment)
  private final val endOfInput: Parser[None.type] = Parser { in =>
    if (!in.atEnd) {
      Error("Expected '/', ':', path literal character, or end of input", in)
    } else {
      Success(None, in)
    }
  }

  private final val template: Parser[Template] = '/'.withFailureMessage("Template must start with a slash") ~>
    segments ~ verb.? <~ endOfInput ^^ { case segments ~ maybeVerb =>
      Template(segments, maybeVerb)
    }
}
