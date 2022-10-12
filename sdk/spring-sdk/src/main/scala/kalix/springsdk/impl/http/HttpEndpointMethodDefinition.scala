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

package kalix.springsdk.impl.http

import akka.http.scaladsl.model.Uri.Path
import akka.http.scaladsl.model.{
  ErrorInfo,
  HttpMethod,
  HttpMethods,
  IllegalRequestException,
  RequestEntityAcceptance,
  StatusCodes,
  Uri
}
import com.google.api.HttpRule.PatternCase
import com.google.api.annotations.AnnotationsProto
import com.google.api.http.{ CustomHttpPattern, HttpRule }
import com.google.api.{ AnnotationsProto => JavaAnnotationsProto, HttpRule => JavaHttpRule }
import com.google.protobuf.{ Descriptors, DynamicMessage }
import com.google.protobuf.Descriptors.{ Descriptor, FieldDescriptor, MethodDescriptor, ServiceDescriptor }
import com.google.protobuf.descriptor.{ MethodOptions => spbMethodOptions }
import com.google.protobuf.util.{ Durations, JsonFormat, Timestamps }
import org.slf4j.LoggerFactory

import java.net.URLDecoder
import java.nio.charset.StandardCharsets.UTF_8
import java.util.regex.Matcher
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

/**
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
  type PathParameterEffect = (FieldDescriptor, Option[Any]) => Unit
  type ExtractPathParameters = (Matcher, PathParameterEffect) => Unit

  // This is used to support the "*" custom pattern
  val ANY_METHOD = HttpMethod.custom(
    name = "ANY",
    safe = false,
    idempotent = false,
    requestEntityAcceptance = RequestEntityAcceptance.Tolerated)

  /**
   * Extracts a HTTP endpoint definition, or throws a ReportableError if the definition is invalid
   */
  def extractForService(serviceDescriptor: ServiceDescriptor): Seq[HttpEndpointMethodDefinition] = {
    for {
      methodDescriptor <- serviceDescriptor.getMethods.asScala.toSeq
      ruleBinding <- ruleBindings(serviceDescriptor, methodDescriptor)
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

  private[http] def ruleBindings(service: ServiceDescriptor, method: MethodDescriptor): Seq[HttpRule] = {

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
        log.info(s"Using configured HTTP API endpoint using [$rule]")
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
      throw new Exception(
        "HttpApi.invalidSelector(rule.selector, methDesc)"
      ) // FIXME fix all the exceptions to useful outputs

    // Validate pattern
    val (mp, pattern) = {
      import HttpMethods._
      import HttpRule.Pattern._

      rule.pattern match {
        case Empty           => throw new Exception("HttpApi.missingPattern(methDesc)")
        case Get(pattern)    => (GET, pattern)
        case Put(pattern)    => (PUT, pattern)
        case Post(pattern)   => (POST, pattern)
        case Delete(pattern) => (DELETE, pattern)
        case Patch(pattern)  => (PATCH, pattern)
        case Custom(chp) =>
          if (chp.kind == "*")
            (ANY_METHOD, chp.path) // FIXME is "path" the same as "pattern" for the other kinds? Is an empty kind valid?
          else throw new Exception("HttpApi.invalidCustomKind(chp.kind, methDesc)")
      }
    }
    val (template, extractor) = parsePathExtractor(pattern, methDesc)

    // Validate body value
    val bd =
      rule.body match {
        case "" => methDesc.getInputType
        case "*" =>
          if (!mp.isEntityAccepted)
            throw new Exception("HttpApi.requestBodyNotAccepted(mp, body = \"*\", methDesc)")
          else
            methDesc.getInputType
        case fieldName =>
          val field = lookupFieldByName(methDesc.getInputType, fieldName)
          if (field == null)
            throw new Exception("HttpApi.requestBodyUnknownField(fieldName, methDesc)")
          else if (field.isRepeated)
            throw new Exception("HttpApi.requestBodyRepeatedField(fieldName, methDesc)")
          else if (!mp.isEntityAccepted)
            throw new Exception("HttpApi.requestBodyNotAccepted(mp, body = fieldName, methDesc)")
          else
            field.getMessageType
      }

    // Validate response body value
    val rd =
      rule.responseBody match {
        case "" => None
        case fieldName =>
          lookupFieldByName(methDesc.getOutputType, fieldName) match {
            case null  => throw new Exception("HttpApi.responseBodyUnknownField(fieldName, methDesc)")
            case field => Some(field)
          }
      }

    if (rule.additionalBindings.exists(_.additionalBindings.nonEmpty))
      throw new Exception("HttpApi.nestedAdditionalBindings(methDesc)")

    (mp, template, extractor, bd, rd)
  }

  final def parsePathExtractor(
      pattern: String,
      methDesc: MethodDescriptor): (PathTemplateParser.ParsedTemplate, ExtractPathParameters) = {
    val template =
      try PathTemplateParser.parse(pattern)
      catch {
        case e: PathTemplateParser.PathTemplateParseException =>
          throw new Exception("HttpApi.pathTemplateParseFailed(e.prettyPrint, methDesc)") //FIXME
      }
    val pathFieldParsers = template.fields.iterator
      .map {
        case tv @ PathTemplateParser.TemplateVariable(fieldName :: Nil, _) =>
          lookupFieldByName(methDesc.getInputType, fieldName) match {
            case null =>
              throw new Exception("HttpApi.pathUnknownField(fieldName, methDesc)") //FIXME
            case field =>
              if (field.isMapField)
                throw new Exception("HttpApi.pathMapField(fieldName, methDesc)") //FIXME
              else if (field.isRepeated)
                throw new Exception("HttpApi.pathRepeatedField(fieldName, methDesc)") //FIXME
              else {
                val notSupported =
                  (message: String) => throw new Exception(s"""HttpApi.notSupportedYet(
                      "HTTP API path for [${methDesc.getFullName}]: $message",
                      "",
                      List(methDesc))""")
                (tv, field, HttpEndpointMethod.suitableParserFor(field)(notSupported))
              }
          }
        case multi =>
          // todo implement field paths properly
          throw new Exception(s"""HttpApi.notSupportedYet(
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

}

final case class HttpEndpointMethodDefinition private (
    methodDescriptor: MethodDescriptor,
    rule: HttpRule,
    methodPattern: HttpMethod,
    pathTemplate: PathTemplateParser.ParsedTemplate,
    pathExtractor: HttpEndpointMethodDefinition.ExtractPathParameters,
    bodyDescriptor: Descriptor,
    responseBodyDescriptor: Option[FieldDescriptor]) {

  private def isAny(descriptor: Descriptors.Descriptor): Boolean =
    //Note: descriptor doesn't implement 'equals', hence the fullName comparison
    descriptor.getFullName == "google.protobuf.Any"

  val inputIsAny: Boolean = isAny(methodDescriptor.getInputType)
  val ruleBodyField: Option[FieldDescriptor] = {
    // used for each request when used, so cache
    if (rule.body == "" || rule.body == "*") None
    else Option(lookupRequestFieldByName(rule.body))
  }
  val ruleBodyFieldIsAny: Boolean = ruleBodyField.exists(fieldDesc => isAny(fieldDesc.getMessageType))

  val jsonParser =
    JsonFormat.parser
      .usingTypeRegistry(JsonFormat.TypeRegistry.newBuilder.add(bodyDescriptor).build())
      .ignoringUnknownFields()
  //usingRecursionLimit(…).

  val jsonPrinter = JsonFormat.printer
    .usingTypeRegistry(JsonFormat.TypeRegistry.newBuilder.add(methodDescriptor.getOutputType).build())
    .includingDefaultValueFields()
    .omittingInsignificantWhitespace()
  //printingEnumsAsInts() // If you enable this, you need to fix the output for responseBody as well
  //preservingProtoFieldNames(). // If you enable this, you need to fix the output for responseBody structs as well
  //sortingMapKeys().

  val isHttpBodyResponse: Boolean = methodDescriptor.getOutputType.getFullName == "google.api.HttpBody"
  val isAnyResponse: Boolean = methodDescriptor.getOutputType.getFullName == "google.protobuf.Any"
  //val instrumentedRequest = InstrumentedHttpRequest(pathTemplate.path, methodDescriptor)

  def lookupRequestFieldByName(fieldName: String) =
    HttpEndpointMethodDefinition.lookupFieldByName(methodDescriptor.getInputType, fieldName)
  // FIXME these belong to request handling, not definition?

  // Making this a method so we can ensure it's used the same way
  def pathMatcher(path: Uri.Path): Matcher =
    pathTemplate.regex.pattern
      .matcher(
        path.toString()
      ) // FIXME path.toString is costly, and using Regexes are too, switch to using a generated parser instead

  def matches(path: Uri.Path): Boolean =
    pathMatcher(path).matches()

  def lookupRequestFieldByPath(selector: String): Descriptors.FieldDescriptor =
    HttpEndpointMethodDefinition.lookupFieldByPath(methodDescriptor.getInputType, selector)

  def parsePathParametersInto(path: Path, inputBuilder: DynamicMessage.Builder): Unit = {
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

  // We use this to signal to the requestor that there's something wrong with the request
  private final val requestError: String => Nothing = s =>
    throw IllegalRequestException(StatusCodes.BadRequest, new ErrorInfo(s))

  def parseRequestParametersInto(query: Map[String, List[String]], inputBuilder: DynamicMessage.Builder): Unit =
    query.foreach { case (selector, values) =>
      if (values.nonEmpty) {
        lookupRequestFieldByPath(selector) match {
          case null => requestError(s"Query parameter [$selector] refers to a non-existent field.")
          case field if field.getJavaType == FieldDescriptor.JavaType.MESSAGE =>
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
