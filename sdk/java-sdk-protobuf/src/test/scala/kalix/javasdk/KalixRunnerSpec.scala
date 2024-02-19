/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.javasdk

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class KalixRunnerSpec extends AnyWordSpec with Matchers {

  private def createTmpFileMapping(mapping: String) = {
    createTmpFile(s"""
        |version: "3"
        |services:
        |  kalix-runtime:
        |    image: gcr.io/kalix-public/kalix-runtime:1.1.32
        |    ports:
        |      - "9000:9000"
        |    extra_hosts:
        |      - "host.docker.internal:host-gateway"
        |    environment:
        |      JAVA_TOOL_OPTIONS: >
        |        -Dkalix.dev-mode.service-port-mappings.$mapping
        |""".stripMargin)
  }

  private def createTmpFile(fileContent: String) = {
    // create a tmp file in target to make it visible to 'DockerComposeUtils'
    val dockerComposeFile = File.createTempFile("kalix-runner-compose-", ".yml", new File("target"))
    dockerComposeFile.deleteOnExit()
    val bw = new BufferedWriter(new FileWriter(dockerComposeFile))
    bw.write(fileContent)
    bw.close()
    // path to file needs to be relative to user.dir, eg: target/file
    dockerComposeFile.getAbsolutePath.replace(sys.props("user.dir") + "/", "")
  }

  "KalixRunner" should {
    "add local discovery config for 0.0.0.0:port in dev-mode" in {

      val dockerComposeFile = createTmpFileMapping("foo=9001")

      val config = ConfigFactory.parseString(s"""
           |kalix {
           |  system {} # needed because we build the main config from kalix.system
           |  dev-mode.docker-compose-file=$dockerComposeFile
           |}
           |""".stripMargin)

      val prepared = KalixRunner.prepareConfig(config)
      val clientConfig = prepared.getConfig("akka.grpc.client.foo")
      clientConfig.getString("host") shouldBe "0.0.0.0"
      clientConfig.getInt("port") shouldBe 9001
    }

    "add local discovery config for somehost:port in dev-mode" in {

      val dockerComposeFile = createTmpFileMapping("foo=somehost:9001")
      val config = ConfigFactory.parseString(s"""
           |kalix {
           |  system {} # needed because we build the main config from kalix.system
           |  dev-mode.docker-compose-file=$dockerComposeFile
           |}
           |""".stripMargin)

      val prepared = KalixRunner.prepareConfig(config)
      val clientConfig = prepared.getConfig("akka.grpc.client.foo")
      clientConfig.getString("host") shouldBe "0.0.0.0"
      clientConfig.getInt("port") shouldBe 9001
    }

    "add local discovery config for some.host:port in dev-mode" in {

      val dockerComposeFile = createTmpFileMapping("foo=some.host:9001")

      val config = ConfigFactory.parseString(s"""
           |kalix {
           |  system {} # needed because we build the main config from kalix.system
           |  dev-mode.service-port-mappings.foo = "some.host:9001"
           |  dev-mode.docker-compose-file=$dockerComposeFile
           |}
           |""".stripMargin)

      val prepared = KalixRunner.prepareConfig(config)
      val clientConfig = prepared.getConfig("akka.grpc.client.foo")
      clientConfig.getString("host") shouldBe "0.0.0.0"
      clientConfig.getInt("port") shouldBe 9001
    }

    "fail config parsing if invalid host:port format" in {

      val dockerComposeFile = createTmpFileMapping("foo=some.host-9001")

      val config = ConfigFactory.parseString(s"""
           |kalix {
           |  system {} # needed because we build the main config from kalix.system
           |  dev-mode.docker-compose-file=$dockerComposeFile
           |}
           |""".stripMargin)

      intercept[IllegalArgumentException] {
        KalixRunner.prepareConfig(config)
      }.getMessage shouldBe "Invalid service port mapping: some.host-9001"
    }

    "reconfigure user function port with docker-compose value" in {

      val fileContent =
        """
          |version: "3"
          |services:
          |  kalix-runtime:
          |    image: gcr.io/kalix-public/kalix-runtime:1.1.32
          |    ports:
          |      - "9000:9000"
          |    extra_hosts:
          |      - "host.docker.internal:host-gateway"
          |    environment:
          |      USER_SERVICE_PORT: 8082
        |""".stripMargin

      val dockerComposeFile = createTmpFile(fileContent)

      val config = ConfigFactory.parseString(s"""
           |kalix {
           |  system {} # needed because we build the main config from kalix.system
           |  dev-mode.docker-compose-file=$dockerComposeFile
           |}
           |""".stripMargin)

      val prepared = KalixRunner.prepareConfig(config)
      prepared.getInt("kalix.user-function-port") shouldBe 8082
    }

  }
}
