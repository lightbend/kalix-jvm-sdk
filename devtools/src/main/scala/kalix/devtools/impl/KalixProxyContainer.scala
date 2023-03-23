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

import com.typesafe.config.Config
import kalix.devtools.BuildInfo
import kalix.devtools.impl.KalixProxyContainer.KalixProxyContainerConfig
import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.DockerImageName

object KalixProxyContainer {

  object KalixProxyContainerConfig {
    def apply(config: Config): KalixProxyContainerConfig = {

      // FIXME: core Kalix should be able to read dev-mode.conf from root folder as well
      // as such, we can kalix.user-function-port configured to something else without touching the prod config
      val userFunctionPort = config.getInt("kalix.user-function-port")
      val containerConfig = config.getConfig("kalix.dev-mode.proxy-container")

      KalixProxyContainerConfig(
        proxyImage = containerConfig.getString("proxy-image"),
        proxyPort = containerConfig.getInt("proxy-port"),
        userFunctionPort = userFunctionPort,
        serviceName = containerConfig.getString("service-name"),
        aclEnabled = containerConfig.getBoolean("acl-enabled"),
        viewFeaturesAll = containerConfig.getBoolean("view-features-all"),
        brokerConfigFile = containerConfig.getString("broker-config-file"),
        pubsubEmulatorHost = containerConfig.getString("pubsub-emulator-host"))

    }
  }

  case class KalixProxyContainerConfig(
      proxyImage: String,
      proxyPort: Int,
      userFunctionPort: Int,
      serviceName: String,
      aclEnabled: Boolean,
      viewFeaturesAll: Boolean,
      brokerConfigFile: String,
      pubsubEmulatorHost: String)

  val logger = LoggerFactory.getLogger(classOf[KalixProxyContainer])

  def apply(config: KalixProxyContainerConfig): KalixProxyContainer = {

    val dockerImage: DockerImageName =
      if (config.proxyImage.trim.nonEmpty) {
        logger.info("Using custom proxy image [{}]", config.proxyImage)
        DockerImageName.parse(config.proxyImage)
      } else {
        DockerImageName.parse(BuildInfo.proxyImage).withTag(BuildInfo.proxyVersion)
      }

    new KalixProxyContainer(dockerImage, config)
  }

}

class KalixProxyContainer private (image: DockerImageName, config: KalixProxyContainerConfig)
    extends GenericContainer[KalixProxyContainer](image) {

  private val containerLogger = LoggerFactory.getLogger("kalix-proxy-server")
  containerLogger.info("KalixProxyContainer config : {}", config)
  withLogConsumer(new Slf4jLogConsumer(containerLogger))

  val proxyPort = config.proxyPort
  val userFunctionPort = config.userFunctionPort
  addFixedExposedPort(proxyPort, proxyPort)

  // JVM are that should be passed to the proxy container on start-up
  withCommand("-Dconfig.resource=dev-mode.conf -Dlogback.configurationFile=logback-dev-mode.xml")

  withEnv("HTTP_PORT", String.valueOf(proxyPort))
  withEnv("USER_FUNCTION_HOST", "host.testcontainers.internal")
  withEnv("USER_FUNCTION_PORT", String.valueOf(userFunctionPort))

  withEnv("ACL_ENABLED", config.aclEnabled.toString)
  withEnv("VIEW_FEATURES_ALL", config.viewFeaturesAll.toString)

  if (config.serviceName.nonEmpty) {
    containerLogger.info("Service name set to {}", config.serviceName)
    withEnv("SERVICE_NAME", config.serviceName)

    // use service name as container instance name (instead of random one from testcontainer)
    withCreateContainerCmdModifier(cmd => cmd.withName(config.serviceName))
  }

  if (config.brokerConfigFile.nonEmpty)
    withEnv("BROKER_CONFIG_FILE", config.brokerConfigFile)

  // FIXME: we will probably need to as a HOST
  if (config.pubsubEmulatorHost.nonEmpty)
    withEnv("PUBSUB_EMULATOR_HOST", config.pubsubEmulatorHost)

  override def start(): Unit = {
    containerLogger.info("Starting Kalix Server...")
    Testcontainers.exposeHostPorts(userFunctionPort)
    super.start()
  }

  override def stop(): Unit = {
    containerLogger.info("Stopping Kalix Server...")
    super.stop()
  }
}
