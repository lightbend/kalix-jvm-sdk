/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.devtools.impl

import com.typesafe.config.{ Config, ConfigFactory }

import scala.jdk.CollectionConverters._

object DevModeSettings {

  val portMappingsKeyPrefix = "kalix.dev-mode.service-port-mappings"
  val tracingConfigEnabled = "kalix.proxy.telemetry.tracing.enabled"
  val tracingConfigEndpoint = "kalix.proxy.telemetry.tracing.collector-endpoint"

  def fromConfig(config: Config): DevModeSettings =
    addPortsFromConfig(config)

  private def addPortsFromConfig(config: Config): DevModeSettings = {
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

  private def tracingSDKConfig(tracingConf: Option[Int]): Config = {
    tracingConf match {
      case Some(port) => ConfigFactory.parseString(s"""
           |kalix.telemetry.tracing.collector-endpoint="http://localhost:$port"
           |""".stripMargin)
      case _ => ConfigFactory.empty()
    }

  }

  private def grpcClientConfig(serviceName: String, host: String, port: Int): Config =
    ConfigFactory.parseString(s"""
         |akka.grpc.client.$serviceName {
         |  service-discovery {
         |    service-name = "$serviceName"
         |  }
         |  host = "$host"
         |  port = $port
         |  use-tls = false
         |}
         |""".stripMargin)

  private def restClientConfig(serviceName: String, host: String, port: Int) =
    ConfigFactory.parseString(s"""${portMappingsKeyPrefix}.$serviceName="$host:$port"""")

  def addDevModeConfig(mainConfig: Config): Config = {
    // enrich config with extra dev-mode service port mappings if applicable
    DockerComposeUtils
      .fromConfig(mainConfig)
      .map { dcu =>
        // read user-function port from docker-compose and overwrite the main config
        val adaptedConfig =
          ConfigFactory
            .parseString(s"kalix.user-function-port = ${dcu.userFunctionPort}")
            .withFallback(tracingSDKConfig(dcu.tracingConfig))
            .withFallback(mainConfig)

        dcu.servicesHostAndPortMap.foldLeft(adaptedConfig) { case (main, (serviceName, hostAndPort)) =>
          // DockerComposeUtils (dcu) is only available when running locally,
          // therefore should always use localhost (0.0.0.0)
          val host = "0.0.0.0"
          val port = HostAndPort.extractPort(hostAndPort)
          // when configuring through DockerComposeUtils, we need two parts:
          // a config for a gRPC client and a direct config for REST WebClient
          main
            .withFallback(grpcClientConfig(serviceName, host, port)
              .withFallback(restClientConfig(serviceName, host, port)))

        }
      }
      .getOrElse {
        // when not relying on DockerComposeUtils, we should still look for dev-mode settings
        // and translate them to gRPC clients
        if (mainConfig.hasPath(portMappingsKeyPrefix)) {
          val portMappings =
            mainConfig.getConfig(portMappingsKeyPrefix).entrySet().asScala
          portMappings.foldLeft(mainConfig) { case (main, entry) =>
            val (host, port) = HostAndPort.extract(entry.getValue.unwrapped().toString)
            main
              .withFallback(grpcClientConfig(entry.getKey, host, port))
              .withFallback(restClientConfig(entry.getKey, host, port))
          }

        } else
          mainConfig
      }
  }

  def empty: DevModeSettings = DevModeSettings(Map.empty, None)
}

case class DevModeSettings(portMappings: Map[String, String], tracingPort: Option[Int]) {
  def addMapping(key: String, value: String): DevModeSettings =
    this.copy(portMappings = portMappings + (key -> value))
}
