/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.devtools.impl
object UserServicePortExtractor {

  private val ExtractPort = """USER_SERVICE_PORT:.*?(\d+).?""".r
  private val ExtractLegacyPort = """USER_FUNCTION_PORT:.*?(\d+).?""".r

  def unapply(line: String): Option[Int] =
    line.trim match {
      case ExtractPort(port)       => Some(port.toInt)
      case ExtractLegacyPort(port) => Some(port.toInt)
      case _                       => None
    }
}
