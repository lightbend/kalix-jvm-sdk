/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen
package java

import scala.io.Source
import scala.util.Using

import _root_.java.io.PrintWriter
import _root_.java.nio.file.{ Files, Paths }
import _root_.java.time.Duration
import _root_.java.util.concurrent.TimeUnit.MINUTES

import com.typesafe.config.ConfigFactory
import org.scalacheck.Gen
import org.slf4j.LoggerFactory
import org.testcontainers.containers.Container.ExecResult
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.images.builder.ImageFromDockerfile
import org.testcontainers.utility.LazyFuture
import com.typesafe.config.ConfigException.Generic
import org.testcontainers.containers.ExecInContainerPattern
import org.testcontainers.containers.Network
import requests.Requester
import org.testcontainers.utility.MountableFile

class SourceGeneratorSuite extends munit.FunSuite {
  val config        = ConfigFactory.load()
  val mavenJavaPath = Paths.get(config.getString("akkaserverless-maven-java.path"))
  val jarPath       = Paths.get(config.getString("java-codegen.jar"))
  val proxyImage    = config.getString("akkaserverless-proxy.image")
  val logger        = LoggerFactory.getLogger(classOf[SourceGeneratorSuite]);

  val codegenImage =
    new ImageFromDockerfile("akkasls-codegen-java-test", false)
      .withFileFromClasspath("Dockerfile", "Dockerfile")
      .withFileFromClasspath("scripts", "scripts")
      // .withFileFromClasspath("entrypoint.sh", "entrypoint.sh")
      .withFileFromPath("akkasls-codegen-java_2.13-1.0-SNAPSHOT.jar", jarPath)
      .withFileFromPath("akkaserverless-maven-java", mavenJavaPath)

  class AkkaslsJavaCodegenContainer
      extends GenericContainer[AkkaslsJavaCodegenContainer](codegenImage)

  class AkkaslsProxyContainer extends GenericContainer[AkkaslsProxyContainer](proxyImage)

  val logConsumer = new Slf4jLogConsumer(logger);
  logger.info("Waiting for docker image to be built. This can take several minutes")
  // We manually await the docker image to avoid timeouts waiting on Maven install
  val imageName = codegenImage.get(10, MINUTES)
  logger.info(s"Docker image built and tagged as [${imageName}]")

  // Define simple dockerised deployment with proxy and container for generated project
  val network = Network.newNetwork()
  val codegenContainer =
    new AkkaslsJavaCodegenContainer()
      .withNetwork(network)
      .withNetworkAliases("generated-entity")
      .withEnv("HOST", "0.0.0.0")
  val proxyContainer = new AkkaslsProxyContainer()
    .withNetwork(network)
    .withNetworkAliases("proxy")
    .withExposedPorts(9000)
    .withEnv("USER_FUNCTION_HOST", "generated-entity")
    .withEnv("USER_FUNCTION_PORT", "8080")

  // Start the codegen container (the proxy fails its health checks until a service is available)
  codegenContainer.start()

  test("verify unmodified generated entity") {
    val entityName = "unmodified-entity"

    // Generate a new entity within the codegen container
    assertSuccessful(generateEntity(codegenContainer)(entityName))

    // Start the entity gRPC server
    assertSuccessful(
      codegenContainer.execInContainer(
        "./scripts/start-entity.sh",
        entityName
      )
    )

    // Start the proxy
    proxyContainer.start()
    val proxyUrl = s"http://${proxyContainer.getHost}:${proxyContainer.getMappedPort(9000)}"

    // Eventually, a GetValue request should return a 500 error, with our generated error message
    var getResult = retryUntil[requests.Response](_.statusCode == 500) {
      requests.post(
        s"$proxyUrl/com.example.MyServiceEntity/GetValue",
        check = false,
        data = """{"entityId": "test-entity-id"}""",
        headers = Map("Content-Type" -> "application/json")
      )
    }
    assertEquals(getResult.statusCode, 500)
    assertEquals(getResult.text(), "The command handler for `GetValue` is not implemented, yet")

    // Kill the gRPC server, and stop the proxy
    assertSuccessful(
      codegenContainer.execInContainer(
        "./scripts/stop-entity.sh",
        entityName
      )
    )
    proxyContainer.stop()
  }

  test("verify simple implementation from generated entity") {
    val entityName = "simple-impl-entity"

    // Generate a new entity within the codegen container
    assertSuccessful(generateEntity(codegenContainer)(entityName))

    // Build the initial implementation
    assertSuccessful(
      codegenContainer.execInContainer(
        "bash",
        "-c",
        s"cd ${entityName} && mvn compile"
      )
    )

    // Stream generated MyEntityImpl, and replace function bodies with simple implementations
    val implFile = Files.createTempFile("generated-entity-impl", ".java")
    val implContainerPath =
      s"/home/${entityName}/src/main/java/com/example/persistence/MyEntityImpl.java"
    Using(new PrintWriter(implFile.toFile())) { writer =>
      codegenContainer.copyFileFromContainer(
        implContainerPath,
        Source
          .fromInputStream(_)
          .getLines()
          .flatMap {
            case """        throw ctx.fail("The command handler for `GetValue` is not implemented, yet");""" =>
              Seq("""        throw ctx.fail("My modified error message!!!");""")
            case line => Seq(line)
          }
          .foreach { line =>
            println(line)
            writer.write(s"${line}\n")
          }
      )
    }
    codegenContainer.copyFileToContainer(MountableFile.forHostPath(implFile), implContainerPath)

    // Start the entity gRPC server
    assertSuccessful(
      codegenContainer.execInContainer(
        "./scripts/start-entity.sh",
        entityName
      )
    )

    // Start the proxy
    proxyContainer.start()
    val proxyUrl = s"http://${proxyContainer.getHost}:${proxyContainer.getMappedPort(9000)}"

    // Eventually, a GetValue request should return a 500 error, with our generated error message
    var getResult = retryUntil[requests.Response](_.statusCode == 500) {
      requests.post(
        s"$proxyUrl/com.example.MyServiceEntity/GetValue",
        check = false,
        data = """{"entityId": "test-entity-id"}""",
        headers = Map("Content-Type" -> "application/json")
      )
    }
    assertEquals(getResult.statusCode, 500)
    assertEquals(getResult.text(), "My modified error message!!!")

    // Kill the gRPC server, and stop the proxy
    assertSuccessful(
      codegenContainer.execInContainer(
        "./scripts/stop-entity.sh",
        entityName
      )
    )
    proxyContainer.stop()
  }

  def generateEntity(
      container: AkkaslsJavaCodegenContainer
  )(
      artifactId: String,
      groupId: String = "com.example",
      version: String = "1.0-SNAPSHOT"
  ): ExecResult =
    container.execInContainer(
      "mvn",
      "-B",
      "archetype:generate",
      "-DarchetypeGroupId=com.lightbend",
      "-DarchetypeArtifactId=maven-archetype-akkasls",
      "-DarchetypeVersion=1.0-SNAPSHOT",
      s"-DgroupId=${groupId}",
      s"-DartifactId=${artifactId}",
      s"-Dversion=${version}"
    )

  def assertSuccessful(result: ExecResult) = {
    val success = result.getExitCode() == 0
    if (!success) {
      logger.error(result.getStdout())
    } else {
      logger.info(result.getStdout())
    }
    assert(success, () => result.getStderr())
  }

  def retryUntil[A](condition: A => Boolean, limit: Integer = 20, delay: Long = 1000)(
      function: => A
  ): A =
    Some(function).filter(condition).getOrElse {
      if (limit > 0) {
        Thread.sleep(delay)
        retryUntil(condition, limit - 1, delay)(function)
      } else throw new Exception("Retry limit reached")
    }
}
