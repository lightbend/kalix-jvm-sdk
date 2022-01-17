import sbt._
import sbt.Keys._
import java.io.File

object ExampleSuiteCompilationProject {
  val empty =
    new CompositeProject {
      def componentProjects = Seq.empty[Project]
    }
}

abstract class ExampleSuiteCompilationProject extends CompositeProject {

  def name: String
  def pathToTests: String
  def innerProjects: Seq[Project]

  lazy val root =
    Project(id = name, base = file(pathToTests))
      .aggregate(innerProjects.map(p => p: ProjectReference): _*)

  def componentProjects: Seq[Project] = innerProjects :+ root

  def findProjects: Seq[(File, String)] =
    findProjectsDir(file(pathToTests)).map { file =>
      val name = file.getPath.replace(pathToTests, "").replaceAll("[./]+", "-")
      (file, name)
    }

  private def findProjectsDir(base: File): Seq[File] =
    if (base.listFiles().exists(d => d.isDirectory && d.getName == "proto")) Seq(base)
    else
      base
        .listFiles()
        .filter(f => f.isDirectory && f.getName != "." && f.getName != "..")
        .flatMap(findProjectsDir)

}
