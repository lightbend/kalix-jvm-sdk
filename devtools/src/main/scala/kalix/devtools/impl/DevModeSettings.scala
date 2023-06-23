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

import scala.jdk.CollectionConverters._

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

object DevModeSettings {

  val portMappingsKeyPrefix = "kalix.dev-mode.service-port-mappings"

  def fromConfig(config: Config): DevModeSettings = {

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
            main.withFallback(grpcClientConfig(entry.getKey, host, port))
          }

        } else
          mainConfig
      }
  }

  def empty: DevModeSettings = DevModeSettings(Map.empty)
}

case class DevModeSettings(portMappings: Map[String, String]) {
  def addMapping(key: String, value: String): DevModeSettings =
    DevModeSettings(portMappings + (key -> value))
}
