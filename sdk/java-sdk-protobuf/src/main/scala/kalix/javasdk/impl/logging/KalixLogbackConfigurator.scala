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

package kalix.javasdk.impl.logging

import java.io.File

import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.spi.Configurator
import ch.qos.logback.core.spi.ContextAwareBase

class KalixLogbackConfigurator extends ContextAwareBase with Configurator {

  private val devModeLogbackFile = "src/main/resources/logback-dev-mode.xml"
  // this file path only exist in dev-mode. Once packaged we don't have src/main/resources anymore
  private val devModeLogback = new File(sys.props("user.dir"), devModeLogbackFile)

  // not really related to logging, but we can disable it here as well
  System.setProperty("spring.main.banner-mode", "off")

  if (devModeLogback.exists) {
    // only set it if not already set by user
    if (System.getProperty("logging.config") == null)
      System.setProperty("logging.config", devModeLogbackFile)

    // only set it if not already set by user
    if (System.getProperty("logback.configurationFile") == null)
      System.setProperty("logback.configurationFile", devModeLogbackFile)
  }
  override def configure(loggerContext: LoggerContext) =
    Configurator.ExecutionStatus.DO_NOT_INVOKE_NEXT_IF_ANY
}
