/*
 * Copyright 2021 Lightbend Inc.
 */

package com.akkaserverless.javasdk.impl

object DocLinks {

  private val baseUrl = "https://developer.lightbend.com/docs/akka-serverless"

  private val errorCodes = Map(
    "AS-00112" -> s"$baseUrl/java-services/views.html#_changing_the_view",
    "AS-00402" -> s"$baseUrl/java-services/topic-eventing.html",
    "AS-00406" -> s"$baseUrl/java-services/topic-eventing.html"
  )

  // fallback if not defined in errorCodes
  private val errorCodeCategories = Map(
    "AS-001" -> s"$baseUrl/java-services/views.html",
    "AS-002" -> s"$baseUrl/java-services/value-entity.html",
    "AS-003" -> s"$baseUrl/java-services/eventsourced.html",
    "AS-004" -> s"$baseUrl/java-services/" // no single page for eventing
  )

  def forErrorCode(code: String): Option[String] =
    errorCodes.get(code) match {
      case s @ Some(_) => s
      case None => errorCodeCategories.get(code.take(6))
    }

}
