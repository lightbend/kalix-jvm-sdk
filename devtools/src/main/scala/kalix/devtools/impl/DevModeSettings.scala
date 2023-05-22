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

package kalix.devtools.impl

import scala.jdk.CollectionConverters._
import com.typesafe.config.Config

object DevModeSettings {

  val portMappingsKeyPrefix = "kalix.dev-mode.service-port-mappings"

  def fromConfig(config: Config): DevModeSettings = {

    if (config.hasPath(portMappingsKeyPrefix)) {
      val entries = config
        .getConfig(portMappingsKeyPrefix)
        .entrySet()
        .asScala
        .map { entry => entry.getKey -> entry.getValue.unwrapped() }

      entries.foldLeft(DevModeSettings.empty) {
        case (settings, (key, value: String)) => settings.addMapping(key, value)
        case (_, (key, _)) =>
          val fullKey = portMappingsKeyPrefix + "." + key
          throw new IllegalArgumentException("Invalid config type. Settings '" + fullKey + "' should be of type String")
      }
    } else {
      DevModeSettings.empty
    }

  }

  def empty: DevModeSettings = DevModeSettings(Map.empty)
}

case class DevModeSettings(portMappings: Map[String, String]) {
  def addMapping(key: String, value: String): DevModeSettings =
    DevModeSettings(portMappings + (key -> value))
}
