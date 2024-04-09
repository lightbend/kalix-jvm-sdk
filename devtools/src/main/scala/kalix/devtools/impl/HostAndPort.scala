/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.devtools.impl

object HostAndPort {

  private val HostPortPattern = """([\w+\-_.]+):(\d{1,5})""".r
  private val PortPattern = """(\d{1,5})""".r

  /**
   * When running locally, users can configure service port mappings associating a name and a port. In such a case, we
   * will resolve to 0.0.0.0:port and that's just enough. However, when building a docker-compose file containing more
   * than one service, the config will need to include the docker host address, for example:
   * -Dkalix.dev-mode.service-port-mappings.my-service=host.docker.internal:9001
   *
   * We should not default to host.docker.internal because it might depend on docker environment, for example: Docker
   * Desktop, Podman, Colima, Linux, etc.
   */
  def extract(hostPort: String): (String, Int) = {
    def isValidPort(port: String): Boolean = port.toInt >= 0 && port.toInt <= 65535
    hostPort match {
      case HostPortPattern(h, p) if isValidPort(p) => (h, p.toInt)
      case PortPattern(p) if isValidPort(p)        => ("0.0.0.0", p.toInt)
      case _ =>
        throw new IllegalArgumentException(s"Invalid service port mapping: $hostPort")
    }
  }

  def extractPort(hostAndPort: String): Int = extract(hostAndPort)._2

}
