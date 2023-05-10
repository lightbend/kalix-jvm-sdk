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

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util
import java.util.UUID

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class DockerComposeUtilsSpec extends AnyWordSpec with Matchers {

  private val defaultFile =
    """
      |version: "3"
      |services:
      |  kalix-proxy:
      |    image: gcr.io/kalix-public/kalix-proxy:1.1.8
      |    ports:
      |      - "9000:9000"
      |    extra_hosts:
      |      - "host.docker.internal:host-gateway"
      |    environment:
      |      JAVA_TOOL_OPTIONS: >
      |        -Dconfig.resource=dev-mode.conf
      |        -Dlogback.configurationFile=logback-dev-mode.xml
      |        -Dkalix.dev-mode.service-port-mappings.foo=9001
      |        -Dkalix.dev-mode.service-port-mappings.bar=9002
      |      USER_FUNCTION_HOST:${USER_FUNCTION_HOST:-host.docker.internal}
      |      USER_FUNCTION_PORT:${USER_FUNCTION_PORT:-8081}
      |""".stripMargin

  private def createTmpFile(fileContent: String): String = {
    // write docker-compose.yml to a temporary file
    val dockerComposeFile = File.createTempFile("docker-compose", ".yml")
    dockerComposeFile.deleteOnExit()
    val bw = new BufferedWriter(new FileWriter(dockerComposeFile))
    bw.write(fileContent)
    bw.close()
    dockerComposeFile.getAbsolutePath
  }

  "DockerComposeUtils" should {

    "read user function port from docker-compose file" in {
      val dockerComposeFile = createTmpFile(defaultFile)
      val dockerComposeUtils = DockerComposeUtils(dockerComposeFile)
      dockerComposeUtils.userFunctionPort shouldBe 8081
    }

    "favor USER_FUNCTION_PORT env var if set " in {
      val envVar = Map("USER_FUNCTION_PORT" -> "8082")
      val dockerComposeFile = createTmpFile(defaultFile)
      val dockerComposeUtils = DockerComposeUtils(dockerComposeFile, envVar)
      dockerComposeUtils.userFunctionPort shouldBe 8082
    }

    "default to UF port 8080 when nothing is defined" in {
      val fileContent =
        """
          |version: "3"
          |services:
          |  kalix-proxy:
          |    image: gcr.io/kalix-public/kalix-proxy:1.1.8
          |    ports:
          |      - "9000:9000"
          |    extra_hosts:
          |      - "host.docker.internal:host-gateway"
          |""".stripMargin
      val dockerComposeFile = createTmpFile(fileContent)
      val dockerComposeUtils = DockerComposeUtils(dockerComposeFile)
      dockerComposeUtils.userFunctionPort shouldBe 8080
    }

    "be able read user function port when env var is not used" in {
      val fileWithoutEnvVar =
        """
          |version: "3"
          |services:
          |  kalix-proxy:
          |    image: gcr.io/kalix-public/kalix-proxy:1.1.8
          |    ports:
          |      - "9000:9000"
          |    extra_hosts:
          |      - "host.docker.internal:host-gateway"
          |    environment:
          |      USER_FUNCTION_PORT:8081
          |""".stripMargin

      val dockerComposeFile = createTmpFile(fileWithoutEnvVar)
      val dockerComposeUtils = DockerComposeUtils(dockerComposeFile)
      dockerComposeUtils.userFunctionPort shouldBe 8081
    }

    "read service port mappings from docker-compose file" in {
      val dockerComposeFile = createTmpFile(defaultFile)
      val dockerComposeUtils = DockerComposeUtils(dockerComposeFile)
      dockerComposeUtils.servicePortMappings shouldBe Seq(
        "-Dkalix.dev-mode.service-port-mappings.foo=9001",
        "-Dkalix.dev-mode.service-port-mappings.bar=9002")
    }

    "service mappings is docker-compose file are translated to mappings without host" in {
      val defaultFile =
        """
            |version: "3"
            |services:
            |  kalix-proxy:
            |    environment:
            |      JAVA_TOOL_OPTIONS: >
            |        -Dkalix.dev-mode.service-port-mappings.foo=9001
            |        -Dkalix.dev-mode.service-port-mappings.bar=host.docker.internal:9002
            |        -Dkalix.dev-mode.service-port-mappings.baz=somehost:9003
            |        -Dkalix.dev-mode.service-port-mappings.qux=localhost:9004
            |""".stripMargin

      val dockerComposeFile = createTmpFile(defaultFile)
      val dockerComposeUtils = DockerComposeUtils(dockerComposeFile)
      dockerComposeUtils.localServicePortMappings shouldBe Seq(
        "-Dkalix.dev-mode.service-port-mappings.foo=9001",
        "-Dkalix.dev-mode.service-port-mappings.bar=9002",
        "-Dkalix.dev-mode.service-port-mappings.baz=9003",
        "-Dkalix.dev-mode.service-port-mappings.qux=9004")
    }

    "not fail if docker-compose file is absent" in {
      val dockerComposeUtils = DockerComposeUtils(UUID.randomUUID().toString)
      // in which case it should default to 8080
      dockerComposeUtils.userFunctionPort shouldBe 8080
      dockerComposeUtils.servicePortMappings shouldBe Seq.empty
    }

    "select first UF port when more than one proxy is declared." in {

      val extraProxy =
        """
          |  kalix-proxy-2:
          |    image: gcr.io/kalix-public/kalix-proxy:1.1.8
          |    ports:
          |      - "9000:9000"
          |    extra_hosts:
          |      - "host.docker.internal:host-gateway"
          |    environment:
          |      JAVA_TOOL_OPTIONS: >
          |        -Dconfig.resource=dev-mode.conf
          |        -Dlogback.configurationFile=logback-dev-mode.xml
          |      USER_FUNCTION_HOST:${USER_FUNCTION_HOST:-host.docker.internal}
          |      USER_FUNCTION_PORT:${USER_FUNCTION_PORT:-8082}
          |""".stripMargin

      val dockerComposeFile = createTmpFile(defaultFile + extraProxy)
      val dockerComposeUtils = DockerComposeUtils(dockerComposeFile)
      dockerComposeUtils.userFunctionPort shouldBe 8081
    }

  }
}
