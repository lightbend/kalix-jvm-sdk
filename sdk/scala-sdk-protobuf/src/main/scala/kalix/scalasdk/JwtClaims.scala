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

package kalix.scalasdk

import java.time.Instant

import spray.json.{ JsArray, JsBoolean, JsNumber, JsObject, JsString, JsValue, JsonParser }

/**
 * Representation of JWT claims that have been validated and extracted from the bearer token of a request.
 */
trait JwtClaims {

  /**
   * Returns the names of all the claims in this request.
   *
   * @return
   *   The names of all the claims in this request.
   */
  def allClaimNames: Iterable[String]

  /**
   * Returns all the claims as a map of strings to strings.
   *
   * If the claim is a String claim, the value will be the raw String. For all other types, it will be the value of the
   * claim encoded to JSON.
   *
   * @return
   *   All the claims represented as a map of string claim names to string values.
   */
  def asMap: Map[String, String]

  /**
   * Does this request have any claims that have been validated?
   *
   * @return
   *   true if there are claims.
   */
  def hasClaims: Boolean = allClaimNames.iterator.hasNext

  /**
   * Get the issuer, that is, the `iss` claim, as described in RFC 7519 section 4.1.1.
   *
   * @return
   *   the issuer, if present.
   * @see
   *   [[https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.1 RFC 7519 section 4.1.1]]
   */
  def issuer: Option[String] = getString("iss")

  /**
   * Get the subject, that is, the `sub` claim, as described in RFC 7519 section 4.1.2.
   *
   * @return
   *   the subject, if present.
   * @see
   *   [[https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.2 RFC 7519 section 4.1.2]]
   */
  def subject: Option[String] = getString("sub")

  /**
   * Get the audience, that is, the `aud` claim, as described in RFC 7519 section 4.1.3.
   *
   * @return
   *   the audience, if present.
   * @see
   *   [[https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.3 RFC 7519 section 4.1.3]]
   */
  def audience: Option[String] = getString("aud")

  /**
   * Get the expiration time, that is, the `exp` claim, as described in RFC 7519 section 4.1.4.
   *
   * @return
   *   the expiration time, if present. Returns [[None]] if the value is not a numeric date.
   * @see
   *   [[https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.4 RFC 7519 section 4.1.4]]
   */
  def expirationTime: Option[Instant] = getNumericDate("exp")

  /**
   * Get the not before, that is, the `nbf` claim, as described in RFC 7519 section 4.1.5.
   *
   * @return
   *   the not before, if present. Returns [[None]] if the value is not a numeric date.
   * @see
   *   [[https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.5 RFC 7519 section 4.1.5]]
   */
  def notBefore: Option[Instant] = getNumericDate("nbf")

  /**
   * Get the issued at, that is, the `iat` claim, as described in RFC 7519 section 4.1.6.
   *
   * @return
   *   the issued at, if present. Returns [[None]] if the value is not a numeric date.
   * @see
   *   [[https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.6 RFC 7519 section 4.1.6]]
   */
  def issuedAt: Option[Instant] = getNumericDate("iat")

  /**
   * Get the JWT ID, that is, the `jti` claim, as described in RFC 7519 section 4.1.7.
   *
   * @return
   *   the JWT ID, if present.
   * @see
   *   [[https://datatracker.ietf.org/doc/html/rfc7519#section-4.1.7 RFC 7519 section 4.1.7]]
   */
  def jwtId: Option[String] = getString("jti")

  /**
   * Get the string claim with the given name.
   *
   * Note that if the claim with the given name is not a string claim, this will return the JSON encoding of it.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The string claim, if present.
   */
  def getString(name: String): Option[String]

  /**
   * Get the int claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The integer claim, if present. Returns [[None]] if the claim is not an int or can't be parsed as an int.
   */
  def getInt(name: String): Option[Int] = getNumber(name, _.toInt)

  /**
   * Get the long claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The long claim, if present. Returns [[None]] if the claim is not a long or can't be parsed as an long.
   */
  def getLong(name: String): Option[Long] = getNumber(name, _.toLong)

  /**
   * Get the double claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The double claim, if present. Returns [[None]] if the claim is not a double or can't be parsed as an double.
   */
  def getDouble(name: String): Option[Double] = getNumber(name, _.toDouble)

  /**
   * Get the boolean claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The boolean claim, if present. Returns [[None]] if the claim is not a boolean or can't be parsed as a boolean.
   */
  def getBoolean(name: String): Option[Boolean] = getString(name).flatMap {
    case t if t.equalsIgnoreCase("true")  => Some(true)
    case f if f.equalsIgnoreCase("false") => Some(false)
    case _                                => None
  }

  /**
   * Get the numeric data claim with the given name.
   *
   * Numeric dates are expressed as a number of seconds since epoch, as described in RFC 7519 section 2.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The numeric date claim, if present. Returns [[None]] if the claim is not a numeric date or can't be parsed as a
   *   numeric date.
   * @see
   *   [[https://datatracker.ietf.org/doc/html/rfc7519#section-2 RFC 7519 section 2]]
   */
  def getNumericDate(name: String): Option[Instant] = getLong(name).map(Instant.ofEpochSecond)

  /**
   * Get the object claim with the given name.
   *
   * This returns the claim as a Spray JsObject.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The object claim, if present. Returns [[None]] if the claim is not an object or can't be parsed as an object.
   */
  def getObject(name: String): Option[JsObject] = getString(name).flatMap(value =>
    try JsonParser(value) match {
      case obj: JsObject => Some(obj)
      case _             => None
    } catch {
      case _: JsonParser.ParsingException => None
    })

  /**
   * Get the string list claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The string list claim, if present. Returns [[None]] if the claim is not a JSON array of strings or cannot be
   *   parsed as a JSON array of strings.
   */
  def getStringList(name: String): Option[Seq[String]] = getArray(name, { case JsString(value) => value })

  /**
   * Get the integer list claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The integer list claim, if present. Returns [[None]] if the claim is not a JSON array of integers or cannot be
   *   parsed as a JSON array of integers.
   */
  def getIntegerList(name: String): Option[Seq[Int]] =
    getArray(name, { case JsNumber(value) if value.isValidInt => value.toInt })

  /**
   * Get the long list claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The long list claim, if present. Returns [[None]] if the claim is not a JSON array of longs or cannot be parsed
   *   as a JSON array of longs.
   */
  def getLongList(name: String): Option[Seq[Long]] =
    getArray(name, { case JsNumber(value) if value.isValidLong => value.toLong })

  /**
   * Get the double list claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The double list claim, if present. Returns [[None]] if the claim is not a JSON array of doubles or cannot be
   *   parsed as a JSON array of doubles.
   */
  def getDoubleList(name: String): Option[Seq[Double]] = getArray(name, { case JsNumber(value) => value.toDouble })

  /**
   * Get the boolean list claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The boolean list claim, if present. Returns [[None]] if the claim is not a JSON array of booleans or cannot be
   *   parsed as a JSON array of booleans.
   */
  def getBooleanList(name: String): Option[Seq[Boolean]] = getArray(name, { case JsBoolean(value) => value })

  /**
   * Get the numeric date list claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The numeric date list claim, if present. Returns [[None]] if the claim is not a JSON array of numeric dates or
   *   cannot be parsed as a JSON array of numeric dates.
   */
  def getNumericDateList(name: String): Option[Seq[Instant]] = getLongList(name).map(_.map(Instant.ofEpochSecond))

  /**
   * Get the object list claim with the given name.
   *
   * @param name
   *   The name of the claim.
   * @return
   *   The object list claim, if present. Returns [[None]] if the claim is not a JSON array of objects or cannot be
   *   parsed as a JSON array of objects.
   */
  def getObjectList(name: String): Option[Seq[JsObject]] = getArray(name, { case obj: JsObject => obj })

  private def getNumber[T](name: String, parse: String => T): Option[T] = {
    getString(name).flatMap(value =>
      try Some(parse(value))
      catch {
        case _: NumberFormatException => None
      })
  }

  private def getArray[T](name: String, extract: PartialFunction[JsValue, T]): Option[Seq[T]] = {
    getString(name).flatMap { value =>
      try JsonParser(value) match {
        case JsArray(elements) =>
          Some(elements.map { element =>
            extract.applyOrElse(
              element,
              (_: JsValue) => {
                // We're catching and ignoring this exception anyway, so just use it.
                throw new JsonParser.ParsingException("")
              })
          })
        case _ => None
      } catch {
        case _: JsonParser.ParsingException => None
      }
    }
  }
}
