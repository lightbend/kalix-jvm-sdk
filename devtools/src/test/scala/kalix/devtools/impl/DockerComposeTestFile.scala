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

package kalix.devtools.impl

import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter

object DockerComposeTestFile {

  def createTmpFile(fileContent: String, env: Map[String, String] = Map.empty): String = {
    // write docker-compose.yml to a temporary file
    val userDir = sys.props("user.dir")

    val envFile = new File(new File(userDir, "target"), ".env")
    // if previous exist, we should delete it
    if (envFile.exists()) envFile.delete()

    // create .env file if needed
    if (env.nonEmpty) {
      envFile.deleteOnExit()
      val envBuff = new BufferedWriter(new FileWriter(envFile))
      env.foreach { case (key, value) =>
        envBuff.write(s"$key=$value")
      }
      envBuff.close()
    }

    val dockerComposeFile = File.createTempFile("docker-compose-", ".yml", new File(userDir, "target"))
    dockerComposeFile.deleteOnExit()
    val bw = new BufferedWriter(new FileWriter(dockerComposeFile))
    bw.write(fileContent)
    bw.close()
    dockerComposeFile.getAbsolutePath.replace(userDir + "/", "")
  }
}
