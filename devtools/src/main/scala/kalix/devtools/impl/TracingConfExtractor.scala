package kalix.devtools.impl

import com.typesafe.config.ConfigFactory

object TracingConfExtractor {
  /**
   * Extract the [[DevModeSettings.userFunctionTracingKey]] from a line and returns them as a Seq without
   * the settings prefix.
   */
  def unapply(line: String): Option[TracingConfig] = {
    val tracingConfig =
      line
      .split("-D")
      .foldLeft(TracingConfig.empty) { case (acc, curr) =>
        curr match {
          case curr if curr.startsWith(DevModeSettings.tracingConfigEnabled) =>
            acc.copy(enabled = ConfigFactory.parseString(curr).getBoolean(DevModeSettings.tracingConfigEnabled))
          case curr if curr.startsWith(DevModeSettings.tracingConfigEndpoint) =>
            acc.copy(collectorEndpoint = ConfigFactory.parseString(curr).getString(DevModeSettings.tracingConfigEndpoint))
          case _ => acc
        }
      }
    if (tracingConfig.isEmpty) None
    else {
      Some(tracingConfig)
    }
  }

}

case class TracingConfig(enabled: Boolean, collectorEndpoint: String) {
  def isEmpty: Boolean = !enabled && collectorEndpoint == ""
}
object TracingConfig {
  def empty: TracingConfig =  TracingConfig(enabled = false, "")
}
