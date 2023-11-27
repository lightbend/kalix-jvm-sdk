package kalix

import java.io.File

import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.BuildPluginManager
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugins.annotations._
import org.apache.maven.project.MavenProject
import org.twdata.maven.mojoexecutor.MojoExecutor._

object RunMojo {
  def apply(
      jvmArgs: Array[String],
      mainClass: String,
      log: Log,
      mavenProject: MavenProject,
      mavenSession: MavenSession,
      pluginManager: BuildPluginManager,
      runningSolo: Boolean): Unit = {

    assert(mainClass.trim.nonEmpty, "Main class not set. Kalix maven plugin must have `mainClass` set.")

    if (runningSolo) {
      log.warn("Kalix Runtime won't start.")
      log.warn("--------------------------------------------------------------------------------------")
      log.warn("To test this application locally you should either run it using 'mvn kalix:runAll'")
      log.warn("or start the Kalix Runtime by hand using the provided docker-compose file.")
      log.warn("--------------------------------------------------------------------------------------")
    }

    if (jvmArgs.nonEmpty) {
      log.info("Additional JVM arguments detected: " + jvmArgs.toSeq.mkString(", "))
    }

    /*
     * Lookup for arguments passed to maven using -Dkey=value and rebuild a -D property for each of them.
     */
    def collectSysProperties: Seq[String] =
      sys.env
        .get("MAVEN_CMD_LINE_ARGS")
        .map { args =>
          args.split("\\s+").toSeq.collect {
            case arg if arg.startsWith("-D") => arg
          }
        }
        .getOrElse(Seq.empty)

    val mainArgs =
      Seq(
        element(name("argument"), "-classpath"),
        element(name("classpath")))

    val sysProps =
      collectSysProperties.map { arg =>
        element(name("argument"), arg)
      }

    // when running solo, we force docker-compose file to be none
    val dockerConfig =
      if (runningSolo) Seq(element(name("argument"), "-Dkalix.dev-mode.docker-compose-file=none"))
      else Seq.empty

    val additionalJvmArgs = jvmArgs.filter(_.trim.nonEmpty).map(element(name("argument"), _)).toSeq

    val allArgs =
      mainArgs ++ sysProps ++ additionalJvmArgs ++ dockerConfig :+
      element(name("argument"), mainClass) // mainClass must be last arg

    executeMojo(
      plugin("org.codehaus.mojo", "exec-maven-plugin", "3.0.0"),
      goal("exec"),
      configuration(
        element(name("executable"), "java"),
        element(name("arguments"), allArgs: _*),
        element(
          name("environmentVariables"),
          // needed for the runtime to access the user service on all platforms
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
 * To test this application locally you should either run it using 'mvn kalix:runAll' or start the Kalix Runtime by hand
 * using the provided docker-compose file.
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
class RunMojo extends RunParameters {

  override def execute(): Unit = {
    RunMojo(jvmArgs, mainClass, getLog, mavenProject, mavenSession, pluginManager, runningSolo = true)
  }

}
