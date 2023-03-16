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

package kalix.devmode

import com.typesafe.config.Config
import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

object KalixProxyContainer {

  val logger = LoggerFactory.getLogger(classOf[KalixProxyContainer])

  def apply(config: Config): KalixProxyContainer = {

    val customImage = System.getenv("KALIX_PROXY_IMAGE")
    val dockerImage: DockerImageName =
      if (customImage == null)
        DockerImageName.parse(BuildInfo.proxyImage).withTag(BuildInfo.proxyVersion)
      else {
        logger.info("Using custom proxy image [{}]", customImage)
        DockerImageName.parse(customImage)
      }

    new KalixProxyContainer(dockerImage, config)
  }

}

class KalixProxyContainer private (image: DockerImageName, config: Config)
    extends GenericContainer[KalixProxyContainer](image) {

  val proxyPort = config.getInt("kalix.dev-mode.proxy-port")
  val userFunctionPort = config.getInt("kalix.user-function-port")

  addFixedExposedPort(proxyPort, proxyPort)
  withEnv("HTTP_PORT", String.valueOf(proxyPort))
  withEnv("USER_FUNCTION_HOST", "host.testcontainers.internal")
  withEnv("USER_FUNCTION_PORT", String.valueOf(userFunctionPort))

  withEnv("ACL_ENABLED", config.getBoolean("kalix.dev-mode.acl-enabled").toString)
  withEnv("VIEW_FEATURES_ALL", config.getBoolean("kalix.dev-mode.view-features-all").toString)
  withEnv("SERVICE_NAME", config.getString("kalix.dev-mode.service-name"))

//  waitingFor(Wait.forLogMessage(".*gRPC proxy started.*", 1))

  override def start(): Unit = {
    logger.info("Starting Kalix Proxy...")
    Testcontainers.exposeHostPorts(userFunctionPort)
    super.start()
  }

  override def stop(): Unit = {
    logger.info("Stopping Kalix Proxy...")
    super.stop()
  }
}
