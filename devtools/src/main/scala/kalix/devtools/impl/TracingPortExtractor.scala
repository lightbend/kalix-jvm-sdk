/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.devtools.impl

import com.typesafe.config.ConfigFactory

import scala.util.matching.Regex

object TracingPortExtractor {

  val PortPattern: Regex = """:(\d{4,5})""".r

  /**
   * Extracts the port of the `collector-endpoint` from the docker file (after substituting any variable) if tracing is
   * enabled.
   * @param dockerComposeUtilsLines
   * @return
   */
  def unapply(dockerComposeUtilsLines: Seq[String]): Option[Int] = {

    def isValidPort(port: String): Boolean = port.toInt >= 1023 && port.toInt <= 65535
    def removeColon(port: String): String = port.replace(":", "")

    val lines =
      dockerComposeUtilsLines
        .filter(_.contains("-D"))
        .flatMap(_.split("-D"))
        .filter(_.trim.startsWith("kalix.proxy.telemetry.tracing"))

    lines
      .foldLeft(Option(-1)) {
        case (None, _) => None // once it becomes None, it stays None
        case (port, line) if line.startsWith(DevModeSettings.tracingConfigEnabled) =>
          val enabled = ConfigFactory.parseString(line).getBoolean(DevModeSettings.tracingConfigEnabled)
          port.filter(_ => enabled)
        case (_, line) if line.startsWith(DevModeSettings.tracingConfigEndpoint) =>
          PortPattern
            .findFirstIn(line)
            .map(removeColon)
            .filter(isValidPort)
            .map(_.toInt)
        case _ => None
      }
      .filter(_ > 0)
  }
}
