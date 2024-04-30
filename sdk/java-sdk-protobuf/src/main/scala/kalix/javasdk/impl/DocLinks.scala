/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.impl

case class DocLinks(sdkName: String) {
  private val baseUrl = "https://docs.kalix.io"

  // sdkName is of format e.g. kalix-java-sdk-protobuf
  private val sdkPath = if (sdkName.endsWith("-protobuf")) "java-protobuf" else "java"

  val errorCodes = Map(
    "KLX-00112" -> "views.html#changing",
    "KLX-00415" -> "publishing-subscribing.html#_subscribing_to_state_changes_from_a_value_entity")

  // fallback if not defined in errorCodes
  val errorCodeCategories = Map(
    "KLX-001" -> "views.html",
    "KLX-002" -> "value-entity.html",
    "KLX-003" -> "event-sourced-entities.html",
    "KLX-004" -> "publishing-subscribing.html",
    "KLX-005" -> "replicated-entity-crdt.html", // only pb sdks
    "KLX-006" -> "writing-grpc-descriptors-protobuf.html#_transcoding_http", // only pb sdks
    "KLX-007" -> "using-jwts.html",
    "KLX-008" -> "timers.html",
    "KLX-009" -> "access-control.html",
    "KLX-010" -> "workflows.html", // only java sdk currently
    "KLX-011" -> "actions.html#_actions_as_life_cycle_hooks")

  def forErrorCode(code: String): Option[String] = {
    val page = errorCodes.get(code) match {
      case s @ Some(_) => s
      case None        => errorCodeCategories.get(code.take(7))
    }
    page.map(p => s"$baseUrl/$sdkPath/$p")
  }
}
