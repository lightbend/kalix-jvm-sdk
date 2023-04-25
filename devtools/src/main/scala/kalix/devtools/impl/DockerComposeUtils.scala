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

import scala.sys.process._

object DockerComposeUtils {

  private def execIfFileExists[T](file: String)(block: => T): T =
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

  def start(file: String, userFunctionPort: Int): Unit = {

    execIfFileExists(file) {
      Process(s"docker-compose -f $file up", None, "USER_FUNCTION_PORT" -> userFunctionPort.toString).run
    }

    // shutdown hook to down containers when jvm exits
    sys.addShutdownHook {
      execIfFileExists(file) {
        stop(file)
      }
    }
  }

  private def stop(file: String): Unit = {
    s"docker compose -f $file stop".!
  }
}
