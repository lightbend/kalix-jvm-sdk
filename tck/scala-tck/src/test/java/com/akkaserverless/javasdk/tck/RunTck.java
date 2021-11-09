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

package com.akkaserverless.javasdk.tck;

import akkaserverless.tck.model.Main;
import com.akkaserverless.javasdk.testkit.BuildInfo;
import com.akkaserverless.scalasdk.AkkaServerlessRunner;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public final class RunTck {
  public static final String TCK_IMAGE = "gcr.io/akkaserverless-public/akkaserverless-tck";
  public static final String TCK_VERSION = BuildInfo.proxyVersion();

  public static void main(String[] args) throws Exception {
    AkkaServerlessRunner runner = Main.createAkkaServerless().createRunner();
    runner.run();

    Testcontainers.exposeHostPorts(8080);

    try {
      new GenericContainer<>(DockerImageName.parse(TCK_IMAGE).withTag(TCK_VERSION))
          .withEnv("TCK_SERVICE_HOST", "host.testcontainers.internal")
          .withLogConsumer(new LogConsumer().withRemoveAnsiCodes(false))
          .withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy())
          .withCommand(
              "-Dakkaserverless.tck.ignore-tests.0=value-entity -Dakkaserverless.tck.ignore-tests.1=event-sourced-entity "
                  + "-Dakkaserverless.tck.ignore-tests.2=replicated-entity -Dakkaserverless.tck.ignore-tests.3=eventing "
                  + "-Dakkaserverless.tck.ignore-tests.4=view")
          .start();
    } catch (Exception e) {
      // container failed, exit with failure, assumes forked run
      System.exit(1);
    }

    Await.result(runner.terminate(), Duration.apply(1000, "ms")); // will exit JVM on shutdown
  }

  // implement BaseConsumer so that we can disable the removal of ANSI codes -- full colour output
  static class LogConsumer extends BaseConsumer<LogConsumer> {
    @Override
    public void accept(OutputFrame outputFrame) {
      System.out.print(outputFrame.getUtf8String());
    }
  }
}
