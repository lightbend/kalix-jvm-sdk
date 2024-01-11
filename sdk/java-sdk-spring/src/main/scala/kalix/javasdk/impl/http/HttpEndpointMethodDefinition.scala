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

import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util
import java.util.regex.Matcher

import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

import akka.http.scaladsl.model.ErrorInfo
import akka.http.scaladsl.model.HttpMethod
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.IllegalRequestException
import akka.http.scaladsl.model.ParsingException
import akka.http.scaladsl.model.RequestEntityAcceptance
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.Uri.Path
import com.google.api.HttpRule.PatternCase
import com.google.api.annotations.AnnotationsProto
import com.google.api.http.CustomHttpPattern
import com.google.api.http.HttpRule
import com.google.api.http.HttpRule.Pattern._
import com.google.api.{ AnnotationsProto => JavaAnnotationsProto }
import com.google.api.{ HttpRule => JavaHttpRule }
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FieldDescriptor
import com.google.protobuf.Descriptors.MethodDescriptor
import com.google.protobuf.Descriptors.ServiceDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.descriptor.{ MethodOptions => spbMethodOptions }
import com.google.protobuf.util.Durations
import com.google.protobuf.util.Timestamps
import kalix.javasdk.impl.http.HttpEndpointMethodDefinition.lookupFieldByName
import kalix.javasdk.impl.http.HttpEndpointMethodDefinition.parsingError
import kalix.javasdk.impl.path.PathPatternParseException
import org.slf4j.LoggerFactory

/**
 * INTERNAL API
 *
 * Declarative model of a HTTP endpoint, and the logic to parse and validate it from a descriptor. Each instance
 * corresponds to the HTTP/JSON representation of one gRPC method. Doesn't actually handle any requests, that is up to
 * the [[HttpEndpointMethod]]
 *
 * References: https://cloud.google.com/endpoints/docs/grpc-service-config/reference/rpc/google.api#httprule
 * https://github.com/googleapis/googleapis/blob/master/google/api/http.proto
 * https://github.com/googleapis/googleapis/blob/master/google/api/annotations.proto
 */
object HttpEndpointMethodDefinition {

  private val log = LoggerFactory.getLogger(classOf[HttpEndpointMethodDefinition])

  // For descriptive purposes so it's clear what these types do
  private type PathParameterEffect = (FieldDescriptor, Option[Any]) => Unit
  private type ExtractPathParameters = (Matcher, PathParameterEffect) => Unit

  // This is used to support the "*" custom pattern
  val ANY_METHOD = HttpMethod.custom(
    name = "ANY",
    safe = false,
    idempotent = false,
    requestEntityAcceptance = RequestEntityAcceptance.Tolerated)

  /**
   * INTERNAL API
   *
   * Extracts a HTTP endpoint definition, or throws a ReportableError if the definition is invalid
   */
  def extractForService(serviceDescriptor: ServiceDescriptor): Seq[HttpEndpointMethodDefinition] = {
    for {
      methodDescriptor <- serviceDescriptor.getMethods.asScala.toSeq
      ruleBinding <- ruleBindings(serviceDescriptor, methodDescriptor)
        .filter(_.pattern != HttpRule.Pattern.Empty)
    } yield {
      val (methodPattern, pathTemplate, pathExtractor, bodyDescriptor, responseBodyDescriptor) =
        extractAndValidate(methodDescriptor, ruleBinding)
      new HttpEndpointMethodDefinition(
        methodDescriptor,
        ruleBinding,
        methodPattern,
        pathTemplate,
        pathExtractor,
        bodyDescriptor,
        responseBodyDescriptor)
    }
  }

  private def ruleBindings(service: ServiceDescriptor, method: MethodDescriptor): Seq[HttpRule] = {

    def convertHttpRule(jHttpRule: JavaHttpRule): Option[HttpRule] = {
      try {
        val httpRule = HttpRule.of(
          selector = jHttpRule.getSelector,
          pattern = {
            jHttpRule.getPatternCase match {
              case PatternCase.GET             => HttpRule.Pattern.Get(jHttpRule.getGet)
              case PatternCase.POST            => HttpRule.Pattern.Post(jHttpRule.getPost)
              case PatternCase.PUT             => HttpRule.Pattern.Put(jHttpRule.getPut)
              case PatternCase.PATCH           => HttpRule.Pattern.Patch(jHttpRule.getPatch)
              case PatternCase.DELETE          => HttpRule.Pattern.Delete(jHttpRule.getDelete)
              case PatternCase.PATTERN_NOT_SET => HttpRule.Pattern.Empty
              case PatternCase.CUSTOM =>
                HttpRule.Pattern.Custom(
                  CustomHttpPattern(kind = jHttpRule.getCustom.getKind, path = jHttpRule.getCustom.getPath))
            }
          },
          body = jHttpRule.getBody,
          responseBody = jHttpRule.getResponseBody,
          additionalBindings = jHttpRule.getAdditionalBindingsList.asScala
            .flatMap(convertHttpRule)
            .toSeq // FIXME can we make this tailrec?
        )
        Some(httpRule)
      } catch {
        case NonFatal(e) =>
          log.error(s"Failed to convert $jHttpRule", e)
          None
      }
    }

    def convertMethodOptions(method: MethodDescriptor): spbMethodOptions =
      spbMethodOptions
        .fromJavaProto(method.getOptions)
        .withExtension(AnnotationsProto.http)(
          convertHttpRule(method.getOptions.getExtension(JavaAnnotationsProto.http)))

    val rule = convertMethodOptions(method).extension(AnnotationsProto.http) match {
      case Some(rule) =>
        if (rule.pattern != HttpRule.Pattern.Empty) {
          log.info(s"Using configured HTTP API endpoint using [$rule]")
        }
        rule
      case None =>
        val rule = HttpRule.of(
          selector = method.getFullName, // We know what thing we are proxying
          body = "*", // Parse all input
          responseBody = "", // Include all output
          additionalBindings = Nil, // No need for additional bindings
          pattern = HttpRule.Pattern.Post((Path / service.getFullName / method.getName).toString))
        log.info(s"Using generated HTTP API endpoint using [$rule]")
        rule
    }
    rule +: rule.additionalBindings
  }

  // This method validates the configuration and returns values obtained by parsing the configuration
  private final def extractAndValidate(methDesc: MethodDescriptor, rule: HttpRule)
      : (HttpMethod, PathTemplateParser.ParsedTemplate, ExtractPathParameters, Descriptor, Option[FieldDescriptor]) = {
    // Validate selector
    if (rule.selector != "" && rule.selector != methDesc.getFullName)
      parsingError(s"HTTP API selector [${rule.selector}] is not valid")

    // Validate pattern
    val (mp, pattern) = {
      import HttpMethods._

      rule.pattern match {
        case Empty           => parsingError(s"HTTP API option for [${methDesc.getFullName}] is missing a pattern")
        case Get(pattern)    => (GET, pattern)
        case Put(pattern)    => (PUT, pattern)
        case Post(pattern)   => (POST, pattern)
        case Delete(pattern) => (DELETE, pattern)
        case Patch(pattern)  => (PATCH, pattern)
        case Custom(chp) =>
          if (chp.kind == "*")
            (ANY_METHOD, chp.path) // FIXME is "path" the same as "pattern" for the other kinds? Is an empty kind valid?
          else
            parsingError(
              s"HHTTP API option for [${methDesc.getFullName}] has a custom pattern with an unsupported kind [${chp.kind}]")
      }
    }
    val (template, extractor) = parsePathExtractor(pattern, methDesc)

    // Validate body value
    val bd =
      rule.body match {
        case "" => methDesc.getInputType
        case "*" =>
          if (!mp.isEntityAccepted)
            parsingError(
              s"HTTP API option for [${methDesc.getFullName}] has a body [\"*\"] but [$mp] does not accept a request body")
          else
            methDesc.getInputType
        case fieldName =>
          val field = lookupFieldByName(methDesc.getInputType, fieldName)
          if (field == null)
            parsingError(
              s"HTTP API option for [${methDesc.getFullName}] has a body configured to [$fieldName] but that field does not exist")
          else if (field.isRepeated)
            parsingError(
              s"HTTP API option for [${methDesc.getFullName}] has a body configured to [$fieldName] but that is a repeated field")
          else if (!mp.isEntityAccepted)
            parsingError(
              s"HTTP API option for [${methDesc.getFullName}] has a body [$fieldName] but [$mp] does not accept a request body")
          else
            field.getMessageType
      }

    // Validate response body value
    val rd =
      rule.responseBody match {
        case "" => None
        case fieldName =>
          lookupFieldByName(methDesc.getOutputType, fieldName) match {
            case null =>
              parsingError(
                s"HTTP API option for [${methDesc.getFullName}] has a response body configured to [$fieldName] but that field does not exist")
            case field => Some(field)
          }
      }

    if (rule.additionalBindings.exists(_.additionalBindings.nonEmpty))
      parsingError(s"HTTP API option for [${methDesc.getFullName}] has nested additional bindings")

    (mp, template, extractor, bd, rd)
  }

  private def parsePathExtractor(
      pattern: String,
      methDesc: MethodDescriptor): (PathTemplateParser.ParsedTemplate, ExtractPathParameters) = {
    val template =
      try PathTemplateParser.parse(pattern)
      catch {
        case _: PathPatternParseException =>
          parsingError(s"HTTP API path template for [${methDesc.getFullName}] could not be parsed")
      }
    val pathFieldParsers = template.fields.iterator
      .map {
        case tv @ PathTemplateParser.TemplateVariable(fieldName :: Nil, _) =>
          lookupFieldByName(methDesc.getInputType, fieldName) match {
            case null =>
              parsingError(
                s"HTTP API path template for [${methDesc.getFullName}] references an unknown field named [$fieldName], methDesc)")
            case field =>
              if (field.isMapField)
                parsingError(
                  s"HTTP API path template for [${methDesc.getFullName}] references [$fieldName] but that is a map field")
              else if (field.isRepeated)
                parsingError(
                  s"HTTP API path template for [${methDesc.getFullName}] references [$fieldName] but that is a repeated field")
              else {
                val notSupported =
                  (message: String) => parsingError(s"HTTP API path for [${methDesc.getFullName}]: $message")
                (tv, field, HttpEndpointMethod.suitableParserFor(field)(notSupported))
              }
          }
        case multi =>
          // todo implement field paths properly
          parsingError(s"""HttpApi.notSupportedYet(
            s"HTTP API path template for [${methDesc.getFullName}] references a field path [${multi.fieldPath
            .mkString(".")}]",
            "Referencing sub-fields with field paths is not yet supported.",
            List(methDesc))""")
      }
      .zipWithIndex
      .toList

    (
      template,
      (matcher, effect) => {
        pathFieldParsers.foreach { case ((_, field, parser), idx) =>
          val rawValue = matcher.group(idx + 1)
          // When encoding, we need to be careful to only encode / if it's a single segment variable. But when
          // decoding, it doesn't matter, we decode %2F if it's there regardless.
          val decoded = URLDecoder.decode(rawValue, UTF_8)
          val value = parser(decoded)
          effect(field, value)
        }
      })
  }

  @tailrec private def lookupFieldByPath(desc: Descriptor, selector: String): FieldDescriptor = {
    def splitNext(name: String): (String, String) = {
      val dot = name.indexOf('.')
      if (dot >= 0) {
        (name.substring(0, dot), name.substring(dot + 1))
      } else {
        (name, "")
      }
    }

    splitNext(selector) match {
      case ("", "")        => null
      case (fieldName, "") => lookupFieldByName(desc, fieldName)
      case (fieldName, next) =>
        val field = lookupFieldByName(desc, fieldName)
        if (field == null) null
        else if (field.getMessageType == null) null
        else lookupFieldByPath(field.getMessageType, next)
    }
  }

  // Question: Do we need to handle conversion from JSON names?
  private def lookupFieldByName(desc: Descriptor, selector: String): FieldDescriptor =
    desc.findFieldByName(selector) // TODO potentially start supporting path-like selectors with maximum nesting level?

  private def parsingError(msg: String): Nothing = throw ParsingException(new ErrorInfo(msg))
}

final case class HttpEndpointMethodDefinition private (
    methodDescriptor: MethodDescriptor,
    rule: HttpRule,
    methodPattern: HttpMethod,
    pathTemplate: PathTemplateParser.ParsedTemplate,
    pathExtractor: HttpEndpointMethodDefinition.ExtractPathParameters,
    bodyDescriptor: Descriptor,
    responseBodyDescriptor: Option[FieldDescriptor]) {

  // Making this a method so we can ensure it's used the same way
  private def pathMatcher(path: String): Matcher =
    pathTemplate.regex.pattern
      .matcher(
        path
      ) // FIXME path.toString is costly, and using Regexes are too, switch to using a generated parser instead

  def matches(path: String): Boolean =
    pathMatcher(path).matches()

  private def lookupRequestFieldByPath(selector: String): Descriptors.FieldDescriptor =
    HttpEndpointMethodDefinition.lookupFieldByPath(methodDescriptor.getInputType, selector)

  def parseTypedPathParametersInto(pathVariables: Map[String, ?], inputBuilder: DynamicMessage.Builder): Unit = {

    //TODO fix exceptions msgs
    pathVariables.foreach { case (fieldName, value) =>
      val field = lookupFieldByName(methodDescriptor.getInputType, fieldName) match {
        case null =>
          parsingError(
            s"HTTP API path template for [${methodDescriptor.getFullName}] references an unknown field named [$fieldName], methDesc)")
        case field =>
          if (field.isMapField)
            parsingError(
              s"HTTP API path template for [${methodDescriptor.getFullName}] references [$fieldName] but that is a map field")
          else if (field.isRepeated)
            parsingError(
              s"HTTP API path template for [${methodDescriptor.getFullName}] references [$fieldName] but that is a repeated fieldfield")
          else {
            val notSupported =
              (message: String) => parsingError(s"HTTP API path for [${methodDescriptor.getFullName}]: $message")

            //we don't need a parser, just to check if the type is supported
            HttpEndpointMethod.suitableParserFor(field)(notSupported)
            field
          }
      }

      inputBuilder.setField(field, value)
    }
  }

  def parsePathParametersInto(path: String, inputBuilder: DynamicMessage.Builder): Unit = {
    val matcher = pathMatcher(path)
    matcher.find()
    pathExtractor(
      matcher,
      (field, value) =>
        inputBuilder.setField(
          field,
          value.getOrElse(
            throw new IllegalArgumentException(
              s"Path contains value of wrong type! Expected field of type ${field.getType}."))))
  }

  private val singleStringMessageParsers = Map[String, String => Any](
    "google.protobuf.Timestamp" -> Timestamps.parse,
    "google.protobuf.Duration" -> Durations.parse)

  // We use this to signal to the requester that there's something wrong with the request
  private def requestError(msg: String): Nothing =
    throw IllegalRequestException(StatusCodes.BadRequest, new ErrorInfo(msg))

  def parseTypedRequestParametersInto(
      queryParams: Map[String, util.List[scala.Any]],
      inputBuilder: DynamicMessage.Builder): Unit = {
    queryParams.foreach { case (name, values) =>
      if (!values.isEmpty) {
        lookupRequestFieldByPath(name) match {
          case null => requestError(s"Query parameter [$name] refers to a non-existent field.")
          case field if field.getJavaType == FieldDescriptor.JavaType.MESSAGE =>
            requestError(s"Query parameter [$name] refers to a message type. Only scalar value types are supported.")
          case field if !field.isRepeated && values.size() > 1 =>
            requestError(s"Query parameter [$name] has multiple values for a non-repeated field.")
          case field =>
            if (field.isRepeated) {
              values.forEach(v => {
                inputBuilder.addRepeatedField(field, v)
              })
            } else {
              inputBuilder.setField(field, values.get(0))
            }
        }
      }
    }
  }

  def parseRequestParametersInto(query: Map[String, List[String]], inputBuilder: DynamicMessage.Builder): Unit =
    query.foreach { case (selector, values) =>
      if (values.nonEmpty) {
        lookupRequestFieldByPath(selector) match {
          case null => requestError(s"Query parameter [$selector] refers to a non-existent field.")
          case field if field.getJavaType == FieldDescriptor.JavaType.MESSAGE =>
            //this is actually not supported at the moment: https://github.com/lightbend/kalix-jvm-sdk/issues/1434
            singleStringMessageParsers.get(field.getMessageType.getFullName) match {
              case Some(parser) =>
                try {
                  val parsed = parser(values.head)
                  inputBuilder.setField(field, parsed)
                } catch {
                  case ex: Exception =>
                    requestError(
                      s"Query parameter [$selector] refers to a field of message type [${field.getFullName}], but could not be parsed into that type. ${ex.getMessage}")
                }
              case None =>
                requestError(
                  s"Query parameter [$selector] refers to a message type. Only scalar value types and message types [${singleStringMessageParsers.keys
                    .mkString(", ")}] are supported.")
            }
          case field if !field.isRepeated && values.size > 1 =>
            requestError(s"Query parameter [$selector] has multiple values for a non-repeated field.")
          case field => // FIXME verify that we can set nested fields from the inputBuilder type
            val x = HttpEndpointMethod.suitableParserFor(field)(requestError)
            if (field.isRepeated) {
              values.foreach { v =>
                inputBuilder.addRepeatedField(
                  field,
                  x(v).getOrElse(requestError(s"Malformed query parameter [$selector].")))
              }
            } else
              inputBuilder.setField(
                field,
                x(values.head).getOrElse(
                  requestError(s"Malformed query parameter [$selector]. Expected field of type ${field.getType}.")))
        }
      } // Ignore empty values
    }
}

final case class HttpEndpointMethodParsingException(field: String, methodDesc: Descriptors.MethodDescriptor)
    extends RuntimeException(s"Parsing of field=$field failed for methodDesc=$methodDesc")
