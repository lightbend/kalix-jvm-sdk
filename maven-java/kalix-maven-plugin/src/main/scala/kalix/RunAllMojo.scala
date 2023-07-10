package kalix

import org.apache.maven.plugins.annotations.Execute
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.ResolutionScope

/**
 * Runs the current project. This goal will by default start a Kalix Server (Proxy) and the current application.
 */
@Mojo(name = "runAll", defaultPhase = LifecyclePhase.VALIDATE, requiresDependencyResolution = ResolutionScope.RUNTIME)
@Execute(phase = LifecyclePhase.COMPILE)
class RunAllMojo extends RunParameters {

  override def execute(): Unit = {
    RunMojo(
      jvmArgs,
      mainClass,
      getLog,
      mavenProject,
      mavenSession,
      pluginManager,
      runningSolo = false)
  }
}
