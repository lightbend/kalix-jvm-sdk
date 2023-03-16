package kalix;

import kalix.devtools.impl.KalixProxyContainer;
import kalix.devtools.impl.KalixProxyContainerFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Goal which deploys the current project to the repository and Kalix.
 */
@SuppressWarnings("unused")
@Mojo(name = "run", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class RunMojo extends AbstractMojo {


  @Component
  private MavenProject mavenProject;

  @Component
  private MavenSession mavenSession;

  @Component
  private BuildPluginManager pluginManager;

  @Parameter(property = "mainClass")
  private String mainClass;

  @Parameter(property = "kalix.dev-mode.log-config")
  private String devLogConfig;

  @Parameter(property = "kalix.dev-mode.user-function-port")
  private int userFunctionPort = 8080;

  @Parameter(property = "kalix.dev-mode.enabled")
  private boolean devModeEnabled = true;

  @Parameter(property = "kalix.dev-mode.proxyPort")
  private int proxyPort = 9000;

  @Parameter(property = "kalix.dev-mode.proxyImage")
  private String proxyImage = "";

  @Parameter(property = "kalix.dev-mode.serviceName")
  private String serviceName = "";

  private final Log log = getLog();

  public void execute() throws MojoExecutionException {

    // TODO: when restarting, it's possible that testcontainers haven't yet freed the port from previous run
    // therefore we need to check if the port is free and it not, add some artificial delay. And we should keep trying
    // and notifying the user that we are waiting for the port.

    if (devModeEnabled) {
      log.info("Kalix DevMode is enabled");
      KalixProxyContainer.KalixProxyContainerConfig config =
        new KalixProxyContainer.KalixProxyContainerConfig(
          proxyImage,
          proxyPort,
          userFunctionPort,
          serviceName,
          // TODO
          false, // ACL enabled
          false, // viewFeaturesAll
          "", // broker config file
          "" // pubsub emulator
        );

      KalixProxyContainer container = KalixProxyContainerFactory.apply(config);

      // REVIEW: do this properly, for example, pick a thread from a pool if possible
      Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
          container.start();
        }
      });

      thread.start();

    } else
      log.info("Kalix DevMode is disabled. Kalix Server won't start.");

    log.info("Using dev logging config: " + devLogConfig);

    executeMojo(
      plugin("org.codehaus.mojo", "exec-maven-plugin", "3.0.0"),
      goal("exec"),
      configuration(
        element(name("executable"), "java"),
        element(name("arguments"),
          element(name("argument"), "-classpath"),
          element(name("classpath")),

          element(name("argument"), "-Dlogback.configurationFile=" + devLogConfig),
          // when using SpringBoot, logback config is passed using logging.config
          element(name("argument"), "-Dlogging.config=" + devLogConfig),
          element(name("argument"), "-Dkalix.user-function-port=" + userFunctionPort),
          element(name("argument"), mainClass)
        ),
        element(name("environmentVariables"),
          // needed for the proxy to access the user function on all platforms
          // should we make this configurable?
          element("HOST", "0.0.0.0"),
          // If using Spring, don't print it's banner as this is run by kalix:run, not spring-boot:run
          element("SPRING_MAIN_BANNER-MODE", "off")
        )
      ),
      executionEnvironment(
        mavenProject,
        mavenSession,
        pluginManager
      )
    );
  }

}
