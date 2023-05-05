package kalix

import org.apache.maven.plugins.annotations.Parameter

trait DockerParameters {

  /**
   * Defines the docker-compose file to use.
   */
  @Parameter(property = "kalix.dev-mode.docker-compose-file", defaultValue = "docker-compose.yml")
  protected var dockerComposeFile: String = ""
}
