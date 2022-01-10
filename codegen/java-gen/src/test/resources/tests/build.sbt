val sdk = ProjectRef(file("../../../../../.."), "sdkJava")
val testkit = ProjectRef(file("../../../../../.."), "sdkJavaTestKit")

val modules: CompositeProject = new CompositeProject {
  val inner = findProjects(file(".")).map(f =>
    Project("test" + f.getPath.replaceAll("[./]+", "-"), f)
      .dependsOn(sdk % "compile", testkit % "test")
      .settings(
        Compile / unmanagedSourceDirectories ++= Seq("generated-managed", "generated-unmanaged").map(baseDirectory.value / _),
        Test / unmanagedSourceDirectories ++= Seq("generated-test-managed", "generated-test-unmanaged").map(baseDirectory.value / _),
        Compile / PB.protoSources += baseDirectory.value / "proto",
        akkaGrpcGeneratedLanguages := Seq(AkkaGrpc.Java),
      )
      .enablePlugins(ProtocPlugin, AkkaGrpcPlugin)
  )
  val root = project
    .in(file("."))
    .aggregate(inner.map(p => p: ProjectReference): _*)

  def componentProjects: Seq[Project] = inner :+ root

  import java.io.File
  def findProjects(base: File): Seq[File] =
    if (base.listFiles().exists(d => d.isDirectory && d.getName == "proto")) Seq(base)
    else {
      base.listFiles()
        .filter(f => f.isDirectory && f.getName != "." && f.getName != "..")
        .flatMap(findProjects)
    }
}
