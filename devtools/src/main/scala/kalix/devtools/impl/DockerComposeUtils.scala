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

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import scala.io.Source
import scala.jdk.CollectionConverters._
import scala.sys.process._
import com.typesafe.config.Config

import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import scala.util.Success
import scala.util.Try

object DockerComposeUtils {

  def apply(file: String): DockerComposeUtils = DockerComposeUtils(file, Map.empty)

  def fromConfig(config: Config): Option[DockerComposeUtils] =
    Option(config.getString("kalix.dev-mode.docker-compose-file"))
      .filter(_.trim.toLowerCase != "none")
      .filter { file => new File(sys.props("user.dir"), file).exists() }
      .map { file => DockerComposeUtils(file, sys.env) }
}

case class DockerComposeUtils(file: String, envVar: Map[String, String]) {

  // mostly for using from Java code
  def this(file: String) = this(file, Map.empty)

  @volatile private var started = false

  private def execIfFileExists[T](block: => T): T =
    if (Files.exists(Paths.get(file)))
      block
    else {
      val extraMsg =
        if (file == "docker-compose.yml")
          "This file is included in the project by default. Check if it was not deleted by mistake."
        else
          "Check if your build is configured correctly and the file name was not mistyped."

      throw new IllegalArgumentException(s"File '$file' does not exist. $extraMsg")
    }

  // read the file once and cache the lines
  // we will need to iterate over it more than once
  private lazy val lines: Seq[String] =
    if (Files.exists(Paths.get(file))) {
      val src = Source.fromFile(file)
      try {
        src.getLines().toList
      } finally {
        src.close()
      }
    } else {
      Seq.empty
    }

  // docker-compose sends some of its output to stderr even when it's not an error
  // to avoid sending the wrong message to users, we redirect to stdout
  // unfortunately, real errors won't be logged as errors anymore
  // this is an issue with some versions of docker-compose, latest version seems to have it fixed
  // (see https://github.com/docker/compose/issues/7346)
  private val processLogger = ProcessLogger(out => println(out))

  // FIXME: process output is being printed in sbt console as error
  // note to self: this is seems to be similar to sbt native packager printing errors when build docker images
  def start(): Unit =
    execIfFileExists {
      val pullFlag = dockerImagesAvailable match {
        case Success(true) => "--pull=always"
        case _ =>
          val fmt = DateTimeFormatter.ofPattern("HH:mm:ss:SSS") //In line with logback-dev-mode.xml
          println(
            s"${LocalTime.now().format(fmt)} WARN Kalix plugin can NOT `docker pull` the `latest` kalix-runtime. Using your local version. The plugin is trying to download the latest `kalix-runtime` to ensure that you can use the latest `kalix-sdk`. Check your internet connection.")
          ""
      }
      val proc = Process(s"docker-compose -f $file up $pullFlag", None).run(processLogger)
      started = proc.isAlive()
      // shutdown hook to down containers when jvm exits
      sys.addShutdownHook {
        execIfFileExists {
          stop()
        }
      }
    }

  //At the moment we store kalix-runtime in gcr.io
  private def dockerImagesAvailable: Try[Boolean] = {
    val url = new URL("https://gcr.io/v2/")
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("GET")
    Try {
      val responseCode = connection.getResponseCode()
      responseCode == HttpURLConnection.HTTP_OK
    }
  }

  def stop(): Unit =
    if (started)
      execIfFileExists {
        Process(s"docker-compose -f $file stop", None).run(processLogger)
      }

  def stopAndWait(): Int =
    if (started)
      execIfFileExists {
        Process(s"docker-compose -f $file stop", None).!(processLogger)
      }
    else 0

  def userFunctionPort: Int =
    envVar
      .get("USER_FUNCTION_PORT")
      .map(_.toInt)
      .orElse(userFunctionPortFromFile)
      .getOrElse(8080)

  private def userFunctionPortFromFile: Option[Int] =
    lines.collectFirst { case UserFunctionPortExtractor(port) => port }

  /**
   * Extract all lines starting with [[DevModeSettings.portMappingsKeyPrefix]] The returned Seq only contains the
   * service name and the mapped host and port, eg: some-service=somehost:9001
   */
  private def servicePortMappings: Seq[String] =
    lines.flatten {
      case ServicePortMappingsExtractor(mappings) => mappings
      case _                                      => Seq.empty
    }

  /**
   * Returns a Map from service name to host:port.
   */
  def servicesHostAndPortMap: Map[String, String] =
    servicePortMappings.map { mapping =>
      mapping.split("=") match {
        case Array(serviceName, hostAndPort) => serviceName -> hostAndPort
        case _                               => throw new IllegalArgumentException(s"Invalid port mapping: $mapping")
      }
    }.toMap

}
