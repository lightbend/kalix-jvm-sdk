/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
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
