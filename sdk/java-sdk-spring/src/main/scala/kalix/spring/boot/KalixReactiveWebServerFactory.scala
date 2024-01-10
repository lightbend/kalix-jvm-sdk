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

package kalix.spring.boot

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

import kalix.spring.impl.KalixSpringApplication
import org.slf4j.LoggerFactory
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory
import org.springframework.boot.web.server.WebServer
import org.springframework.http.server.reactive.HttpHandler

class KalixReactiveWebServerFactory(kalixApp: KalixSpringApplication) extends ReactiveWebServerFactory {

  private val logger = LoggerFactory.getLogger(this.getClass)

  override def getWebServer(httpHandler: HttpHandler): WebServer = {

    // Spring will initialise the server quite earlier in the process and
    // if it fails to wire the other components, it will call WebServer.stop before even calling WebServer.start
    // That doesn't play well for us (sbt/ActorSystem/Akka Http), therefore we use this flag to keep track of it.
    @volatile var started = false

    logger.info("Instantiating Kalix ReactiveWebServer")
    new WebServer {
      override def start(): Unit = {
        logger.info("Starting Kalix ReactiveWebServer...")
        kalixApp.start()
        started = true
      }

      override def stop(): Unit = {
        if (started) {
          logger.info("Stopping Kalix ReactiveWebServer...")
          try {
            Await.ready(kalixApp.stop(), 10.seconds)
          } finally {
            started = false
          }
        }
      }

      override def getPort: Int = kalixApp.port
    }
  }

}
