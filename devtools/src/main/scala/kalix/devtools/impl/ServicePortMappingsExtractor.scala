/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
