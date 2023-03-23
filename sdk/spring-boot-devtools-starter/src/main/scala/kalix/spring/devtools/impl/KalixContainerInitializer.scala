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

package kalix.spring.devtools.impl

import java.io.File

import scala.concurrent.Future

import com.typesafe.config.ConfigFactory
import kalix.devtools.impl.KalixProxyContainer
import kalix.devtools.impl.KalixProxyContainer.KalixProxyContainerConfig
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent

class KalixContainerInitializer
    extends ApplicationContextInitializer[ConfigurableApplicationContext]
    with ApplicationListener[ContextClosedEvent] {

  private val logger = LoggerFactory.getLogger(this.getClass)

  private val config = {
    val mainConfig = ConfigFactory.load()

    val devConfigFile = new File(System.getProperty("user.dir"), "dev-mode.conf")

    if (devConfigFile.exists()) {
      logger.info(s"Found Kalix dev-mode config: $devConfigFile")
      val devConfig = ConfigFactory.parseFile(devConfigFile)
      devConfig.withFallback(mainConfig)
    } else mainConfig

  }.resolve()

  private val container = KalixProxyContainer(KalixProxyContainerConfig(config))

  override def initialize(applicationContext: ConfigurableApplicationContext): Unit = {
    if (config.hasPath("kalix.dev-mode.enabled") && config.getBoolean("kalix.dev-mode.enabled")) {
      logger.info("Starting DevMode Kalix Server")
      // start it asynchronously
      import scala.concurrent.ExecutionContext.Implicits.global
      Future { container.start() }
    }
  }

  override def onApplicationEvent(event: ContextClosedEvent): Unit =
    container.stop()
}
