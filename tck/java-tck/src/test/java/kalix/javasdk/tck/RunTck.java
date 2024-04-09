/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.javasdk.tck;

import kalix.javasdk.KalixRunner;
import kalix.javasdk.testkit.BuildInfo;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.BaseConsumer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

public final class RunTck {
  public static final String TCK_IMAGE = "gcr.io/kalix-public/kalix-tck";
  public static final String TCK_VERSION = BuildInfo.runtimeVersion();

  public static void main(String[] args) throws Exception {
    KalixRunner runner = JavaSdkTck.SERVICE.createRunner();
    runner.run();

    Testcontainers.exposeHostPorts(8080);

    try {
      String version = TCK_VERSION;
      if (version.endsWith("-SNAPSHOT")) version = version.substring(0, version.length() - 9);
      new GenericContainer<>(DockerImageName.parse(TCK_IMAGE).withTag(version))
          .withEnv("TCK_SERVICE_HOST", "host.testcontainers.internal")
          .withLogConsumer(new LogConsumer().withRemoveAnsiCodes(false))
          .withStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy())
          .start();
    } catch (Exception e) {
      // container failed, exit with failure, assumes forked run
      System.exit(1);
    }

    runner.terminate().toCompletableFuture().get(); // will exit JVM on shutdown
  }

  // implement BaseConsumer so that we can disable the removal of ANSI codes -- full colour output
  static class LogConsumer extends BaseConsumer<LogConsumer> {
    @Override
    public void accept(OutputFrame outputFrame) {
      System.out.print(outputFrame.getUtf8String());
    }
  }
}
