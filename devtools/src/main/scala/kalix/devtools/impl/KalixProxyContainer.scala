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

import kalix.devtools.BuildInfo
import kalix.devtools.impl.KalixProxyContainer.KalixProxyContainerConfig
import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.utility.DockerImageName

object KalixProxyContainer {

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
      if (config.proxyImage.trim.nonEmpty)
        DockerImageName.parse(config.proxyImage)
      else
        DockerImageName.parse(BuildInfo.proxyImage).withTag(BuildInfo.proxyVersion)

    new KalixProxyContainer(dockerImage, config)
  }

}

object KalixProxyContainerFactory {
  def apply(config: KalixProxyContainerConfig): KalixProxyContainer =
    KalixProxyContainer(config)
}

class KalixProxyContainer private (image: DockerImageName, config: KalixProxyContainerConfig)
    extends GenericContainer[KalixProxyContainer](image) {

  private val containerLogger = LoggerFactory.getLogger("kalix-proxy-server")
  withLogConsumer(new Slf4jLogConsumer(containerLogger))

  val proxyPort = config.proxyPort
  val userFunctionPort = config.userFunctionPort
  addFixedExposedPort(proxyPort, proxyPort)

  withEnv("HTTP_PORT", String.valueOf(proxyPort))
  withEnv("USER_FUNCTION_HOST", "host.testcontainers.internal")
  withEnv("USER_FUNCTION_PORT", String.valueOf(userFunctionPort))

  withEnv("ACL_ENABLED", config.aclEnabled.toString)
  withEnv("VIEW_FEATURES_ALL", config.viewFeaturesAll.toString)

  if (config.serviceName.nonEmpty) {
    withEnv("SERVICE_NAME", config.serviceName)

    // use service name as container instance name (instead of random one from testcontainer)
    withCreateContainerCmdModifier(cmd => cmd.withName(config.serviceName))
  }

  // FIXME: users must also indicate a folder to mount as volume
  if (config.brokerConfigFile.nonEmpty)
    withEnv("BROKER_CONFIG_FILE", config.brokerConfigFile)

  // JVM are that should be passed to the proxy container on start-up
  val containerArgs =
    Seq("-Dconfig.resource=dev-mode.conf", "-Dlogback.configurationFile=logback-dev-mode.xml")

  val pubSubContainerArg =
    if (config.pubsubEmulatorHost.nonEmpty) {
      withEnv("PUBSUB_EMULATOR_HOST", config.pubsubEmulatorHost)
      Some("-Dkalix.proxy.eventing.support=google-pubsub-emulator")
    } else None

  val finalArgs = containerArgs ++ pubSubContainerArg
  withCommand(finalArgs: _*)

  @volatile
  private var started: Boolean = false

  override def start(): Unit = {
    containerLogger.info("Starting Kalix Server...")
    containerLogger.info("Using proxy image [{}]", image)
    containerLogger.info("KalixProxyContainer config : {}", config)
    Testcontainers.exposeHostPorts(userFunctionPort)
    super.start()
    started = true
  }

  override def stop(): Unit =
    if (started) {
      containerLogger.info("Stopping Kalix Server...")
      super.stop()
    }
}
