package kalix

import scala.concurrent.Future

import kalix.devtools.impl.KalixProxyContainer
import kalix.devtools.impl.KalixProxyContainerFactory
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.BuildPluginManager
import org.apache.maven.plugins.annotations._
import org.apache.maven.project.MavenProject
import org.twdata.maven.mojoexecutor.MojoExecutor._

/**
 * Runs the current project. This goal will by default start a Kalix Server (Proxy) and the current application. Kalix
 * Server bootstrap is skipped if dev-mode is disabled, see `devModeEnabled` property.
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
class RunMojo extends AbstractMojo {
  @Component
  private var mavenProject: MavenProject = null

  @Component
  private var mavenSession: MavenSession = null

  @Component
  private var pluginManager: BuildPluginManager = null

  @Parameter(property = "mainClass")
  private var mainClass = ""

  @Parameter(property = "kalix.log-config")
  private var logConfig = null

  @Parameter(property = "kalix.user-function-port")
  private var userFunctionPort = 8080

  /**
   * Enables dev-mode
   */
  @Parameter(property = "kalix.dev-mode.enabled")
  private var devModeEnabled = true

  @Parameter(property = "kalix.dev-mode.proxyPort")
  private var proxyPort = 9000

  @Parameter(property = "kalix.dev-mode.proxyImage")
  private var proxyImage = ""

  @Parameter(property = "kalix.dev-mode.serviceName")
  private var serviceName = ""

  private var log = getLog

  override def execute(): Unit = {
    log.info("Using dev logging config: " + logConfig)
    startKalixProxy()
    startUserFunction()
  }

  private def startKalixProxy(): Unit = {
    // TODO: when restarting, it's possible that testcontainers haven't yet freed the port from previous run
    // therefore we need to check if the port is free and it not, add some artificial delay. And we should keep trying
    // and notifying the user that we are waiting for the port.
    if (devModeEnabled) {
      log.info("Kalix DevMode is enabled")
      val config = new KalixProxyContainer.KalixProxyContainerConfig(
        proxyImage,
        proxyPort,
        userFunctionPort,
        serviceName, // TODO
        false, // ACL enabled
        false, // viewFeaturesAll
        "", // broker config file
        ""
      ) // pubsub emulator

      val container = KalixProxyContainerFactory.apply(config)
      import scala.concurrent.ExecutionContext.Implicits.global
      Future(container.start())

    } else log.info("Kalix DevMode is disabled. Kalix Server won't start.")
  }

  private def startUserFunction(): Unit =
    executeMojo(
      plugin("org.codehaus.mojo", "exec-maven-plugin", "3.0.0"),
      goal("exec"),
      configuration(
        element(name("executable"), "java"),
        element(
          name("arguments"),
          element(name("argument"), "-classpath"),
          element(name("classpath")),
          element(
            name("argument"),
            "-Dlogback.configurationFile=" + logConfig
          ), // when using SpringBoot, logback config is passed using logging.config
          element(name("argument"), "-Dlogging.config=" + logConfig),
          element(name("argument"), "-Dkalix.user-function-port=" + userFunctionPort),
          element(name("argument"), mainClass)),
        element(
          name("environmentVariables"),
          // needed for the proxy to access the user function on all platforms
          // should we make this configurable?
          element("HOST", "0.0.0.0"),
          // If using Spring, don't print it's banner as this is run by kalix:run, not spring-boot:run
          element("SPRING_MAIN_BANNER-MODE", "off"))),
      executionEnvironment(mavenProject, mavenSession, pluginManager))
}
