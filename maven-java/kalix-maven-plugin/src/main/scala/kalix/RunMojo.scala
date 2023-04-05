package kalix

import scala.concurrent.Future

import kalix.devtools.impl.KalixProxyContainer
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.BuildPluginManager
import org.apache.maven.plugin.logging.Log
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
  private var mainClass: String = ""

  @Parameter(property = "kalix.log-config")
  private var logConfig: String = null

  /**
   * Overrides the user-function port, defaults to 8080
   */
  @Parameter(name = "kalix.dev-mode.user-function-port", property = "kalix.user-function-port")
  private var userFunctionPort: Int = 8080

  /**
   * Enables dev-mode
   */
  @Parameter(property = "kalix.dev-mode.proxy.enabled")
  private var devModeEnabled: Boolean = true

  /**
   * Overrides the proxy port, defaults to 9000
   */
  @Parameter(property = "kalix.dev-mode.proxy.port")
  private var proxyPort: Int = 9000

  @Parameter(property = "kalix.dev-mode.proxy.image")
  private var proxyImage: String = ""

  @Parameter(property = "kalix.dev-mode.service-name")
  private var serviceName: String = ""

  private val log: Log = getLog

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
      val config = KalixProxyContainer.KalixProxyContainerConfig(
        proxyImage,
        proxyPort,
        userFunctionPort,
        serviceName, // TODO
        false, // ACL enabled
        false, // viewFeaturesAll
        "", // broker config file
        ""
      ) // pubsub emulator

      val container = KalixProxyContainer(config)
      import scala.concurrent.ExecutionContext.Implicits.global
      Future(container.start())

    } else log.info("Kalix DevMode is disabled. Kalix Server won't start.")
  }

  private def startUserFunction(): Unit = {

    def when(cond: Boolean)(elements: Element*): Seq[Element] =
      if (cond) elements
      else Seq.empty

    val mainArgs =
      Seq(
        element(name("argument"), "-classpath"),
        element(name("classpath")),
        element(name("argument"), "-Dkalix.user-function-port=" + userFunctionPort),
        element(name("argument"), mainClass))

    val optionalArgs =
      when(logConfig.trim.nonEmpty)(
        element(name("argument"), "-Dlogback.configurationFile=" + logConfig),
        // when using SpringBoot, logback config is passed using logging.config
        element(name("argument"), "-Dlogging.config=" + logConfig))

    executeMojo(
      plugin("org.codehaus.mojo", "exec-maven-plugin", "3.0.0"),
      goal("exec"),
      configuration(
        element(name("executable"), "java"),
        element(name("arguments"), (mainArgs ++ optionalArgs): _*),
        element(
          name("environmentVariables"),
          // needed for the proxy to access the user function on all platforms
          // should we make this configurable?
          element("HOST", "0.0.0.0"),
          // If using Spring, don't print it's banner as this is run by kalix:run, not spring-boot:run
          element("SPRING_MAIN_BANNER-MODE", "off"))),
      executionEnvironment(mavenProject, mavenSession, pluginManager))
  }
}
