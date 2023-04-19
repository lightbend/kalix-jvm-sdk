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

import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

import kalix.devtools.impl.KalixProxyContainer.KalixProxyContainerConfig
import org.slf4j.LoggerFactory
import org.testcontainers.Testcontainers
import org.testcontainers.containers.BindMode
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
      brokerConfigFile: Option[String],
      pubsubEmulatorPort: Option[Int],
      servicePortMappings: Map[String, String])

  def apply(config: KalixProxyContainerConfig): KalixProxyContainer = {
    val dockerImage: DockerImageName = DockerImageName.parse(config.proxyImage)
    new KalixProxyContainer(dockerImage, config)
  }

}

class KalixProxyContainer private (image: DockerImageName, config: KalixProxyContainerConfig)
    extends GenericContainer[KalixProxyContainer](image) {

  private val containerLogger = LoggerFactory.getLogger("kalix-proxy-server")
  withLogConsumer(new Slf4jLogConsumer(containerLogger).withSeparateOutputStreams)

  private val defaultConfigDir = "/conf"
  // make sure that the proxy container can access the local file system
  // we will mount the current directory as /conf and use that as the location for the broker config file
  // or any other config files that we might need in the future
  withFileSystemBind(".", defaultConfigDir, BindMode.READ_ONLY)

  private val proxyPort = config.proxyPort
  private val userFunctionPort = config.userFunctionPort
  addFixedExposedPort(proxyPort, proxyPort)

  withEnv("HTTP_PORT", String.valueOf(proxyPort))
  withEnv("USER_FUNCTION_HOST", "host.testcontainers.internal")
  withEnv("USER_FUNCTION_PORT", String.valueOf(userFunctionPort))

  withEnv("ACL_ENABLED", config.aclEnabled.toString)
  withEnv("VIEW_FEATURES_ALL", config.viewFeaturesAll.toString)

  if (config.serviceName.nonEmpty) {
    withEnv("SERVICE_NAME", config.serviceName)

    // use service name as container instance name (instead of random one from testcontainers)
    withCreateContainerCmdModifier(cmd => cmd.withName(config.serviceName))
  }

  // JVM are that should be passed to the proxy container on start-up
  val containerArgs =
    Seq("-Dconfig.resource=dev-mode.conf", "-Dlogback.configurationFile=logback-dev-mode.xml")

  val eventingArgs =
    (config.brokerConfigFile, config.pubsubEmulatorPort) match {
      case (Some(kafkaConfigFile), None) =>
        containerLogger.info("Configuring eventing support for Kafka broker")
        withEnv("BROKER_CONFIG_FILE", defaultConfigDir + "/" + kafkaConfigFile)
        Some("-Dkalix.proxy.eventing.support=kafka")

      case (None, Some(pubsubPort)) =>
        containerLogger.info("Configuring eventing support for Google Pub/Sub emulator")
        withEnv("PUBSUB_EMULATOR_HOST", "host.testcontainers.internal")
        Some("-Dkalix.proxy.eventing.support=google-pubsub-emulator")

      case (Some(_), Some(_)) =>
        throw new IllegalArgumentException("Only one of brokerConfigFile or pubsubEmulatorPort can be set")

      case _ =>
        containerLogger.info("No eventing support configured")
        None
    }

  val servicePortMappingsArgs =
    config.servicePortMappings.map { case (key, value) =>
      DevModeSettings.servicePortMappingsKeyFor(key, value)
    }

  val finalArgs = containerArgs ++ eventingArgs ++ servicePortMappingsArgs
  withCommand(finalArgs: _*)

  private val kafkaBootstrapServers: Option[String] =
    config.brokerConfigFile.flatMap { file =>

      def toProperties(content: String) = {
        val properties = new Properties()
        properties.load(new StringReader(content))
        properties
      }

      val path = Paths.get(file)
      if (Files.exists(path)) {
        val content = Files.readString(path)
        Option(toProperties(content).getProperty("bootstrap.servers"))
      } else {
        throw new IllegalArgumentException(s"Broker config file [$file] does not exist.")
      }
    }

  // we need to read the file to get the port number and expose it to the container running in testcontainers
  private val kafkaPort: Option[Int] =
    kafkaBootstrapServers.map { value =>
      val split = value.split(":")
      if (split.length != 2) {
        throw new IllegalArgumentException(s"Invalid bootstrap.servers value [$value]. Port number is missing.")
      } else
        split.last.toInt
    }

  private val notDefined = "<not defined>"
  private def renderString(value: String) = if (value.trim.isEmpty) notDefined else value
  private val kafkaRendered =
    for {
      conf <- config.brokerConfigFile
      server <- kafkaBootstrapServers
    } yield s"$conf ($server)"

  val portMappingsRendered =
    if (config.servicePortMappings.nonEmpty)
      config.servicePortMappings
        .map { case (key, value) => s"$key:$value" }
        .mkString(", ")
    else notDefined

  containerLogger.info(s"Starting Kalix Proxy Server container in dev-mode with settings:")
  containerLogger.info("--------------------------------------------------------------------------------------")
  containerLogger.info(s"proxyImage          = $image")
  containerLogger.info(s"proxyPort           = $proxyPort")
  containerLogger.info(s"userFunctionPort    = $userFunctionPort")
  containerLogger.info(s"serviceName         = ${renderString(config.serviceName)}")
  containerLogger.info(s"aclEnabled          = ${config.aclEnabled}")
  containerLogger.info(s"viewFeaturesAll     = ${config.viewFeaturesAll}")
  containerLogger.info(s"brokerConfigFile    = ${kafkaRendered.getOrElse(notDefined)}")
  containerLogger.info(s"pubsubEmulatorHost  = ${config.pubsubEmulatorPort.getOrElse(notDefined)}")
  containerLogger.info(s"servicePortMappings = $portMappingsRendered")
  containerLogger.info("--------------------------------------------------------------------------------------")

  @volatile
  private var started: Boolean = false

  override def start(): Unit = {

    Testcontainers.exposeHostPorts(userFunctionPort)
    config.pubsubEmulatorPort.foreach(Testcontainers.exposeHostPorts(_))
    kafkaPort.foreach(Testcontainers.exposeHostPorts(_))

    super.start()
    started = true
  }

  override def stop(): Unit =
    if (started) {
      containerLogger.info("Stopping Kalix Proxy Server...")
      super.stop()
    }
}
