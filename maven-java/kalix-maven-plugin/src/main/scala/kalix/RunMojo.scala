package kalix

import java.io.File

import kalix.devtools.impl.DockerComposeUtils
import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.BuildPluginManager
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugins.annotations._
import org.apache.maven.project.MavenProject
import org.twdata.maven.mojoexecutor.MojoExecutor._

object RunMojo {
  def apply(
      mainClass: String,
      logConfig: String,
      userFunctionPort: Int,
      servicePortMappings: Seq[String],
      log: Log,
      mavenProject: MavenProject,
      mavenSession: MavenSession,
      pluginManager: BuildPluginManager,
      runningSolo: Boolean): Unit = {

    assert(mainClass.trim.nonEmpty, "Main class not set. Kalix maven plugin must have `mainClass` set.")

    if (runningSolo) {
      log.info("Kalix Proxy won't start.")
      log.info("--------------------------------------------------------------------------------------")
      log.info("To test this application locally you should either run it using 'mvn kalix:runAll'")
      log.info("or start the Kalix Proxy by hand using the provided docker-compose file.")
      log.info("--------------------------------------------------------------------------------------")

    }

    log.info("Starting Kalix Application on port: " + userFunctionPort)

    /*
     * Collect any sys property starting with `kalix` and rebuild a -D property for each of them
     * so we can pass it further to the forked process.
     */
    def collectKalixSysProperties: Seq[String] = {
      sys.props.collect {
        case (key, value) if key.startsWith("kalix") => s"-D$key=$value"
      }.toSeq
    }

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
      (servicePortMappings ++ collectKalixSysProperties).map { arg =>
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

/**
 * Runs the current project. This goal will only start the current application.
 *
 * To test this application locally you should either run it using 'mvn kalix:runAll' or start the Kalix Proxy by hand
 * using the provided docker-compose file.
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
class RunMojo extends RunParameters with DockerParameters {

  override def execute(): Unit = {
    val dockerComposeUtils = DockerComposeUtils(dockerComposeFile, sys.env)
    RunMojo(
      mainClass,
      logConfig,
      dockerComposeUtils.userFunctionPort,
      dockerComposeUtils.servicePortMappings,
      getLog,
      mavenProject,
      mavenSession,
      pluginManager,
      runningSolo = true)
  }

}
