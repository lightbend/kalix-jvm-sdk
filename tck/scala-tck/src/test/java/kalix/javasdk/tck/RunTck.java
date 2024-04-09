/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck;

import kalix.tck.model.TckService;
import kalix.javasdk.testkit.BuildInfo;
import kalix.scalasdk.Kalix;
import kalix.scalasdk.KalixRunner;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;
import scala.concurrent.Await;
import scala.concurrent.duration.Duration;

public final class RunTck {
  public static final String TCK_IMAGE = "gcr.io/kalix-public/kalix-tck";
  public static final String TCK_VERSION = BuildInfo.runtimeVersion();

  public static void main(String[] args) throws Exception {
    Kalix service = TckService.createService();
    KalixRunner runner = service.createRunner();
    runner.run();

    Testcontainers.exposeHostPorts(8080);

    try {
      String version = TCK_VERSION;
      if (version.endsWith("-SNAPSHOT")) version = version.substring(0, version.length() - 9);
      new GenericContainer<>(DockerImageName.parse(TCK_IMAGE).withTag(version))
          .withEnv("TCK_SERVICE_HOST", "host.testcontainers.internal")
          .withLogConsumer(new LogConsumer().withRemoveAnsiCodes(false))
          .withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy())
          .withCommand(
              "-Dkalix.tck.ignore-tests.0=replicated-entity -Dkalix.tck.ignore-tests.1=eventing")
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
