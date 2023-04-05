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
 * Runs the current project. This goal will by default start a Kalix Server (Proxy) and the current application.
 *
 * Kalix Server bootstrap can be skipped by setting `runProxy` to false.
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

  @Parameter(property = "kalix.dev-mode.log-config")
  private var logConfig: String = ""

  /**
   * Overrides the user-function port, defaults to 8080
   */
  @Parameter(property = "kalix.user-function-port")
  private var userFunctionPort: Int = 8080

  /**
   * Runs a Kalix Proxy alongside the current application.
   */
  @Parameter(property = "kalix.dev-mode.run-proxy")
  private var runProxy: Boolean = true

  /**
   * Allows for overriding the port the Kalix Proxy will run. Default to 9000. Useful when running more than one service
   * in dev-mode.
   */
  @Parameter(property = "kalix.dev-mode.proxy-port")
  private var proxyPort: Int = 9000

  /**
   * Allows for overriding the Kalix Proxy image. When empty, the image used when the SDK was built will be selected.
   */
  @Parameter(property = "kalix.dev-mode.proxy-image")
  private var proxyImage: String = ""

  /**
   * Defines a unique identification name for this service. Useful when running more than one service and testing
   * intra-service calls.
   */
  @Parameter(property = "kalix.dev-mode.service-name")
  private var serviceName: String = ""

  /**
   * Enable ACL checks in development. ACL checks are disabled by default during development.
   */
  @Parameter(property = "kalix.dev-mode.acl-enabled")
  private var aclEnabled: Boolean = false

  /**
   * Enable advanced view features (multi-table joins).
   */
  @Parameter(property = "kalix.dev-mode.view-features-all")
  private var viewFeaturesAll: Boolean = false

  /**
   * When configuring with a Kafka broker, this settings should point to a Kafka properties file, eg:
   * /conf/kafka.properties.
   */
  @Parameter(property = "kalix.dev-mode.broker-config-file")
  private var brokerConfigFile: String = ""

  /**
   * When running with a PubSub emulator, this settings must be configured to its host, eg: gcloud-pubsub-emulator.
   */
  @Parameter(property = "kalix.dev-mode.pubsub-emulator-host")
  private var pubsubEmulatorHost: String = ""

  private val log: Log = getLog

  override def execute(): Unit = {
    assert(mainClass.trim.nonEmpty, "Main class not set. Kalix maven plugin must have `mainClass` set to ")
    startKalixProxy()
    startUserFunction()
  }

  private def startKalixProxy(): Unit = {
    // TODO: when restarting, it's possible that testcontainers haven't yet freed the port from previous run
    // therefore we need to check if the port is free and it not, add some artificial delay. And we should keep trying
    // and notifying the user that we are waiting for the port.
    if (runProxy) {
      val config = KalixProxyContainer.KalixProxyContainerConfig(
        proxyImage,
        proxyPort,
        userFunctionPort,
        serviceName,
        aclEnabled,
        viewFeaturesAll,
        brokerConfigFile,
        pubsubEmulatorHost)

      val container = KalixProxyContainer(config)
      import scala.concurrent.ExecutionContext.Implicits.global
      Future(container.start())

    } else {
      log.warn(
        "Kalix Proxy won't start (ie: runProxy = false). " +
        "To test this application locally you should either enable 'runProxy' or start the Kalix Proxy by hand using " +
        "the provided docker-compose file.")
    }
  }

  private def startUserFunction(): Unit = {

    val mainArgs =
      Seq(
        element(name("argument"), "-classpath"),
        element(name("classpath")),
        element(name("argument"), "-Dkalix.user-function-port=" + userFunctionPort),
        element(name("argument"), mainClass))

    log.info("Log configuration: " + logConfig)
    val optionalArgs: Seq[Element] =
      if (logConfig.trim.nonEmpty)
        // when using SpringBoot, logback config is passed using logging.config
        element(name("argument"), "-Dlogging.config=" + logConfig) ::
        element(name("argument"), "-Dlogback.configurationFile=" + logConfig) :: Nil
      else List.empty

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
