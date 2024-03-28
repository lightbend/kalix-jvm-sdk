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

object TracingConfExtractor {

  def unapply(line: String): Option[TracingConfig] = {
    val tracingConfig =
      line
        .split("-D")
        .foldLeft(TracingConfig.empty) { case (acc, curr) =>
          curr match {
            case curr if curr.startsWith(DevModeSettings.tracingConfigEnabled) =>
              acc.copy(enabled = ConfigFactory.parseString(curr).getBoolean(DevModeSettings.tracingConfigEnabled))
            case curr if curr.startsWith(DevModeSettings.tracingConfigEndpoint) =>
              acc.copy(collectorEndpoint =
                Some(ConfigFactory.parseString(curr).getString(DevModeSettings.tracingConfigEndpoint)))
            case _ => acc
          }
        }
    if (tracingConfig.isEmpty) None
    else {
      Some(tracingConfig)
    }
  }

}

case class TracingConfig(enabled: Boolean, collectorEndpoint: Option[String]) {
  def isEmpty: Boolean = !enabled && collectorEndpoint.isEmpty
}
object TracingConfig {
  def empty: TracingConfig = TracingConfig(enabled = false, None)
}
