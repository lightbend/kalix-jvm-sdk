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

object ServicePortMappingsExtractor {

  /**
   * Extracts all occurrences of [[DevModeSettings.portMappingsKeyPrefix]] from a line and returns them as a Seq without
   * the settings prefix.
   */
  def unapply(line: String): Option[Seq[String]] = {
    val portMappings =
      line
        .split("-D")
        .collect {
          case s if s.startsWith(DevModeSettings.portMappingsKeyPrefix) =>
            s.trim.replace(DevModeSettings.portMappingsKeyPrefix + ".", "")
        }

    if (portMappings.nonEmpty) Some(portMappings.toIndexedSeq) else None
  }

}
