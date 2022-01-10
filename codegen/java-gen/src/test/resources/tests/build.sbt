val sdk = ProjectRef(file("../../../../../.."), "sdkJava")

val modules: CompositeProject = new CompositeProject {
  val inner = findProjects(file(".")).map(f =>
    Project("test" + f.getPath.replaceAll("[./]+", "-"), f)
      .dependsOn(sdk % "compile")
      .settings(
        Compile / unmanagedSourceDirectories ++= Seq("generated-managed", "generated-unmanaged").map(baseDirectory.value / _),
        Compile / PB.protoSources += baseDirectory.value / "proto",
        akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
      )
      .enablePlugins(ProtocPlugin, AkkaGrpcPlugin)
  )
  val core = project
    .in(file("."))
    .aggregate(inner.map(p => p: ProjectReference): _*)

  def componentProjects: Seq[Project] = inner :+ core

  import java.io.File
  def findProjects(base: File): Seq[File] =
    if (base.listFiles().exists(d => d.isDirectory && d.getName == "proto")) Seq(base)
    else {
      base.listFiles()
        .filter(f => f.isDirectory && f.getName != "." && f.getName != "..")
        .flatMap(findProjects)
    }
}
