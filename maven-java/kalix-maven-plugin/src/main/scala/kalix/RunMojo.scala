package kalix

import java.io.File
import java.net.BindException
import java.net.ServerSocket
import java.util

import scala.annotation.tailrec
import scala.concurrent.Future
import scala.jdk.CollectionConverters.asScalaBufferConverter
import scala.jdk.CollectionConverters.mapAsScalaMapConverter

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kalix.devtools.BuildInfo
import kalix.devtools.impl.KalixProxyContainer
import kalix.devtools.impl.DevModeSettings
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

  /**
   * Port mappings for local tests.
   */
  @Parameter
  private var servicePortMappings: java.util.Map[String, String] = new util.HashMap()

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

      val allServiceMappings =
        collectServiceMappingsFromSysProps ++
        servicePortMappings.asScala

      val proxyImageToUse =
        if (proxyImage.trim.isEmpty) s"${BuildInfo.proxyImage}:${BuildInfo.proxyVersion}"
        else proxyImage

      val config = KalixProxyContainer.KalixProxyContainerConfig(
        proxyImageToUse,
        proxyPort,
        userFunctionPort,
        serviceName,
        aclEnabled,
        viewFeaturesAll,
        Option(brokerConfigFile).filter(_.trim.nonEmpty), // only set if not empty
        Option(pubsubEmulatorPort).filter(_ > 0), // only set if port is > 0
        allServiceMappings
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

  private def collectServiceMappingsFromSysProps: Map[String, String] =
    sys.props.collect {
      case (key, value) if DevModeSettings.isPortMapping(key) =>
        val serviceName = DevModeSettings.extractServiceName(key)
        serviceName -> value
    }.toMap

  /*
   * Collect any sys property starting with `kalix` and rebuild a -D property for each of them
   * so we can pass it further to the forked process.
   */
  private def collectKalixSysProperties: Seq[String] = {
    sys.props.collect {
      case (key, value) if key.startsWith("kalix") => s"-D$key=$value"
    }.toSeq
  }

  /** Collect port mappings added to pom.xml and make -D properties out of them */
  private def collectServicePortMappings =
    servicePortMappings.asScala.map { case (key, value) =>
      DevModeSettings.portMappingsKeyFor(key, value)
    }.toSeq

  private def startUserFunction(): Unit = {

    log.info("Starting Kalix Application on port: " + userFunctionPort)
    val mainArgs =
      Seq(
        element(name("argument"), "-classpath"),
        element(name("classpath")),
        element(name("argument"), "-Dkalix.user-function-port=" + userFunctionPort))

    val loggingArgs: Seq[Element] = {
      val logConfigFile = new File(logConfig.trim)
      if (logConfigFile.exists) {
        log.info(s"Using logging configuration file: '$logConfigFile' ")
        // when using SpringBoot, logback config is passed using logging.config
        element(name("argument"), "-Dlogging.config=" + logConfig) ::
        element(name("argument"), "-Dlogback.configurationFile=" + logConfig) :: Nil
      } else {
        log.warn(s"Dev mode logging configuration file '$logConfig' not found")
        List.empty
      }
    }

    // first servicePortMappings, then kalixSysProps
    // as it should be possible to override the servicePortMappings with -D args
    val kalixSysProps =
      (collectServicePortMappings ++ collectKalixSysProperties).map { arg =>
        element(name("argument"), arg)
      }

    val allArgs =
      mainArgs ++ loggingArgs ++ kalixSysProps :+
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
