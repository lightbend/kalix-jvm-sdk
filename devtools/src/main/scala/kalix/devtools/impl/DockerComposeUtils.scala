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

import java.nio.file.Files
import java.nio.file.Paths

import scala.io.Source
import scala.sys.process._
import scala.util.Try

object DockerComposeUtils {
  def apply(file: String): DockerComposeUtils = DockerComposeUtils(file, Map.empty)
}
case class DockerComposeUtils(file: String, envVar: Map[String, String]) {

  // mostly for using from Java code
  def this(file: String) = this(file, Map.empty)

  @volatile private var started = false

  private def execIfFileExists[T](block: => T): T = {
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
  }

  // read the file once and cache the lines
  // we will need to iterate over it more than once
  private lazy val lines: Seq[String] = {
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
  }

  def start(): Unit = {
    execIfFileExists {
      val proc = Process(s"docker-compose -f $file up", None).run()
      started = proc.isAlive()
      // shutdown hook to down containers when jvm exits
      sys.addShutdownHook {
        execIfFileExists {
          stop()
        }
      }
    }
  }

  def stop(): Unit =
    if (started) {
      s"docker compose -f $file stop".!
    }

  def readUserFunctionPort: Int = {
    envVar
      .get("USER_FUNCTION_PORT")
      .map(_.toInt)
      .orElse(readUserFunctionPortFromFile)
      .getOrElse(8080)
  }

  private def readUserFunctionPortFromFile: Option[Int] =
    lines.collectFirst { case UserFunctionPortExtractor(port) => port }

  def readServicePortMappings: Seq[String] =
    lines.flatten {
      case ServicePortMappingsExtractor(mappings) => mappings
      case _                                      => Seq.empty
    }
}
