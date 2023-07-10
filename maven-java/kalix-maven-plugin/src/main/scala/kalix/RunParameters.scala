package kalix

import org.apache.maven.execution.MavenSession
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.BuildPluginManager
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

trait RunParameters extends AbstractMojo {

  @Component
  protected var mavenProject: MavenProject = null

  @Component
  protected var mavenSession: MavenSession = null

  @Component
  protected var pluginManager: BuildPluginManager = null

  @Parameter(property = "mainClass")
  protected var mainClass: String = ""

  @Parameter(property = "jvmArgs")
  protected var jvmArgs: Array[String] = Array.empty
}
