package kalix

import java.net.BindException
import java.net.ServerSocket
import java.io.File
import scala.annotation.tailrec
import scala.concurrent.Future
import scala.util.control.NonFatal
import kalix.devtools.BuildInfo
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
@Mojo(name = "run", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
class RunMojo extends AbstractMojo {
  @Component
  private var mavenProject: MavenProject = null

  @Component
  private var mavenSession: MavenSession = null

  @Component
  private var pluginManager: BuildPluginManager = null

  @Parameter(property = "mainClass")
  private var mainClass: String = ""

  @Parameter(property = "kalix.dev-mode.log-config", defaultValue = "src/main/resources/logback-dev-mode.xml")
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
  @Parameter(property = "kalix.dev-mode.service-name", defaultValue = "${project.artifactId}")
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
   * When running with a PubSub, this setting must be configured to its port.
   */
  @Parameter(property = "kalix.dev-mode.pubsub-emulator-port")
  private var pubsubEmulatorPort: Int = 0

  private val log: Log = getLog

  override def execute(): Unit = {
    assert(mainClass.trim.nonEmpty, "Main class not set. Kalix maven plugin must have `mainClass` set to ")
    startKalixProxy()
    startUserFunction()
  }

  @tailrec
  private def checkPortAvailability(proxyPort: Int, count: Int = 0): Unit = {
    def isInUse =
      try {
        new ServerSocket(proxyPort).close()
        false
      } catch { case _: BindException => true }

    if (isInUse) {
      if (count == 20)
        throw new RuntimeException(
          s"Port '$proxyPort' is still in use after 20 seconds. Please make sure that no other service is running on port '$proxyPort'.")

      if (count == 0 || count % 5 == 0)
        log.info(s"Port '$proxyPort' is in use. Waiting for port to become available.")

      Thread.sleep(1000)
      checkPortAvailability(proxyPort, count + 1)
    }
  }

  private def startKalixProxy(): Unit = {

    if (runProxy) {

      checkPortAvailability(proxyPort)

      def renderString(value: String) = if (value.trim.isEmpty) "<not defined>" else value
      def renderPort(value: Int) = if (value == 0) "<not defined>" else value

      val proxyImageToUse =
        if (proxyImage.trim.isEmpty) s"${BuildInfo.proxyImage}:${BuildInfo.proxyVersion}"
        else proxyImage

      log.info(s"Running Kalix in dev-mode with settings:")
      log.info("--------------------------------------------------------------------------------------")
      log.info(s"proxyImage         = $proxyImageToUse")
      log.info(s"proxyPort          = $proxyPort")
      log.info(s"userFunctionPort   = $userFunctionPort")
      log.info(s"serviceName        = ${renderString(serviceName)}")
      log.info(s"aclEnabled         = $aclEnabled")
      log.info(s"viewFeaturesAll    = $viewFeaturesAll")
      log.info(s"brokerConfigFile   = ${renderString(brokerConfigFile)}")
      log.info(s"pubsubEmulatorHost = ${renderPort(pubsubEmulatorPort)}")
      log.info("--------------------------------------------------------------------------------------")

      val config = KalixProxyContainer.KalixProxyContainerConfig(
        proxyImageToUse,
        proxyPort,
        userFunctionPort,
        serviceName,
        aclEnabled,
        viewFeaturesAll,
        brokerConfigFile,
        Option(pubsubEmulatorPort).filter(_ > 0) // only set if port is > 0
      )

      val container = KalixProxyContainer(config)
      import scala.concurrent.ExecutionContext.Implicits.global
      Future(container.start())

      // shutdown hook to stop the container as soon as possible
      // Note: this is not guaranteed to be called, but it's better than nothing
      // also, the main reason it's wrapped in a Future is to ensure that it runs
      // on a thread that shares the same classloader
      sys.addShutdownHook(Future(container.stop()))

    } else {
      log.info("Kalix Proxy won't start (ie: runProxy = false).")
      log.info("--------------------------------------------------------------------------------------")
      log.info("To test this application locally you should either enable 'runProxy'")
      log.info("or start the Kalix Proxy by hand using the provided docker-compose file.")
      log.info("--------------------------------------------------------------------------------------")
    }
  }

  private def startUserFunction(): Unit = {

    log.info("Starting Kalix Application on port: " + userFunctionPort)
    val mainArgs =
      Seq(
        element(name("argument"), "-classpath"),
        element(name("classpath")),
        element(name("argument"), "-Dkalix.user-function-port=" + userFunctionPort))

    val loggingArgs: Seq[Element] =
      if (logConfig.trim.nonEmpty) {
        log.info("Using logging configuration: " + logConfig)
        // when using SpringBoot, logback config is passed using logging.config
        element(name("argument"), "-Dlogging.config=" + logConfig) ::
        element(name("argument"), "-Dlogback.configurationFile=" + logConfig) :: Nil
      } else List.empty

    val allArgs =
      mainArgs ++ loggingArgs :+
      element(name("argument"), mainClass) // mainClass must be last arg

    executeMojo(
      plugin("org.codehaus.mojo", "exec-maven-plugin", "3.0.0"),
      goal("exec"),
      configuration(
        element(name("executable"), "java"),
        element(name("arguments"), allArgs: _*),
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
