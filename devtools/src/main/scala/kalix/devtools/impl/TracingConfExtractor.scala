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

import com.typesafe.config.{ ConfigFactory, ConfigParseOptions, ConfigSyntax }

import scala.util.matching.Regex

object TracingConfExtractor {

  val PortPattern: Regex = """(\d{1,5})""".r

  /**
   * Extracts the port of the `collector-endpoint` from the docker file (after substituting any variable) if tracing is
   * enabled.
   * @param dockerComposeUtilsLines
   * @return
   */
  def unapply(dockerComposeUtilsLines: Seq[String]): Option[Int] = {
    var enabled = false
    var portOpt: Option[Int] = None

    dockerComposeUtilsLines.filter(_.contains("-D")).foreach { line =>
      line.replace(System.lineSeparator(), " ").split("-D").foreach { segment =>
        segment.split(" ").foreach {
          case each if each.startsWith(DevModeSettings.tracingConfigEnabled) =>
            enabled = ConfigFactory.parseString(each).getBoolean(DevModeSettings.tracingConfigEnabled)
          case each if each.startsWith(DevModeSettings.tracingConfigEndpoint) =>
            val url = ConfigFactory
              .parseString(each, ConfigParseOptions.defaults().setSyntax(ConfigSyntax.PROPERTIES))
              .getString(DevModeSettings.tracingConfigEndpoint)
            PortPattern.findFirstIn(url) match {
              case Some(matched) => portOpt = Option(matched.toInt)
              case None          => {}
            }
          case _ => {}
        }
      }
    }
    if (enabled) portOpt
    else None
  }
}
