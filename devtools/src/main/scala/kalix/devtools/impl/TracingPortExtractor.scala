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
      }
      .filter(_ > 0)
  }
}
