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

import scala.concurrent.Future

import com.typesafe.config.Config
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kalix.devtools.impl.KalixProxyContainer
import kalix.devtools.impl.KalixProxyContainer.KalixProxyContainerConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class KalixDevModeServer(config: Config) {

  private val logger = LoggerFactory.getLogger(classOf[KalixDevModeServer])
  private val container: KalixProxyContainer = KalixProxyContainer(KalixProxyContainerConfig(config))

  @PostConstruct
  def start(): Unit = {
    if (config.hasPath("kalix.dev-mode.enabled") && config.getBoolean("kalix.dev-mode.enabled")) {
      logger.info("Starting DevMode Kalix Server")
      // start it asynchronously
      import scala.concurrent.ExecutionContext.Implicits.global
      Future { container.start() }
    }
  }

  @PreDestroy
  def stop(): Unit = container.stop()

}
