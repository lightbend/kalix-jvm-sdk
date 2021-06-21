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

package com.akkaserverless.javasdk.impl

object DocLinks {

  private val baseUrl = "https://developer.lightbend.com/docs/akka-serverless"

  private val errorCodes = Map(
    "AS-00112" -> s"$baseUrl/java/views.html#changing",
    "AS-00402" -> s"$baseUrl/java/topic-eventing.html",
    "AS-00406" -> s"$baseUrl/java/topic-eventing.html"
  )

  // fallback if not defined in errorCodes
  private val errorCodeCategories = Map(
    "AS-001" -> s"$baseUrl/java/views.html",
    "AS-002" -> s"$baseUrl/java/value-entity.html",
    "AS-003" -> s"$baseUrl/java/eventsourced.html",
    "AS-004" -> s"$baseUrl/java/" // no single page for eventing
  )

  def forErrorCode(code: String): Option[String] =
    errorCodes.get(code) match {
      case s @ Some(_) => s
      case None => errorCodeCategories.get(code.take(6))
    }

}
