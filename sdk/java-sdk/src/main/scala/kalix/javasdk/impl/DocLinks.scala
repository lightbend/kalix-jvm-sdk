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

package kalix.javasdk.impl

object DocLinks {

  private val baseUrl = "https://docs.kalix.io"

  private val errorCodes = Map(
    "KLX-00112" -> s"$baseUrl/java/views.html#changing",
    "KLX-00402" -> s"$baseUrl/java/topic-eventing.html",
    "KLX-00406" -> s"$baseUrl/java/topic-eventing.html",
    "KLX-00414" -> s"$baseUrl/java/entity-eventing.html"
    // TODO: docs for value entity eventing (https://github.com/lightbend/kalix-jvm-sdk/issues/121)
    // "KLX-00415" -> s"$baseUrl/java/entity-eventing.html"
  )

  // fallback if not defined in errorCodes
  private val errorCodeCategories = Map(
    "KLX-001" -> s"$baseUrl/java/views.html",
    "KLX-002" -> s"$baseUrl/java/value-entity.html",
    "KLX-003" -> s"$baseUrl/java/eventsourced.html",
    "KLX-004" -> s"$baseUrl/java/", // no single page for eventing
    "KLX-005" -> s"$baseUrl/java/", // no docs yet for replicated entities
    "KLX-006" -> s"$baseUrl/java/proto.html#_transcoding_http", // all HTTP API errors
    "KLX-007" -> s"$baseUrl/services/using-jwts.html",
    "KLX-008" -> s"$baseUrl/java/timers.html")

  def forErrorCode(code: String): Option[String] =
    errorCodes.get(code) match {
      case s @ Some(_) => s
      case None        => errorCodeCategories.get(code.take(6))
    }

}
