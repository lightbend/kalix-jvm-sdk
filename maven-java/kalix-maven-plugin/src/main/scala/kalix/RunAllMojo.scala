package kalix

import kalix.devtools.impl.DockerComposeUtils
import org.apache.maven.plugins.annotations.Execute
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 * Runs the current project. This goal will by default start a Kalix Server (Proxy) and the current application.
 */
@Mojo(name = "runAll", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
class RunAllMojo extends RunParameters with DockerParameters {

  override def execute(): Unit = {

    val dockerComposeUtils = DockerComposeUtils(dockerComposeFile, sys.env)
    dockerComposeUtils.start()

    RunMojo(
      jvmArgs,
      mainClass,
      logConfig,
      dockerComposeUtils.userFunctionPort,
      dockerComposeUtils.localServicePortMappings,
      getLog,
      mavenProject,
      mavenSession,
      pluginManager,
      runningSolo = false)
  }
}
