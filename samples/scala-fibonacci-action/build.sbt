import com.akkaserverless.sbt.AkkaserverlessPlugin.autoImport.generateUnmanaged

name := "fibonacci-action"

organization := "com.akkaseverless.samples"
organizationHomepage := Some(url("https://akkaserverless.com"))
licenses := Seq(
  ("CC0", url("https://creativecommons.org/publicdomain/zero/1.0"))
)

scalaVersion := "2.13.6"

enablePlugins(AkkaGrpcPlugin)
enablePlugins(AkkaserverlessPlugin)

Compile / scalacOptions ++= Seq(
  "-target:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint"
)
Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

Test / parallelExecution := false
Test / testOptions += Tests.Argument("-oDF")
Test / logBuffered := false

run / fork := false
Global / cancelable := false // ctrl-c

Compile / compile := {
  // Make sure 'generateUnmanaged' is executed on each compile, to generate scaffolding code for
  // newly-introduced concepts.
  // After initial generation they are to be maintained manually and will not be overwritten.
  (Compile / generateUnmanaged).value
  (Compile / compile).value
}

// FIXME sdk dependency should be included via sbt-akkaserverless
val AkkaServerlessSdkVersion = System.getProperty("akkaserverless-sdk.version", "0.7.2")

libraryDependencies ++= Seq(
  "com.akkaserverless" %% "akkaserverless-scala-sdk" % AkkaServerlessSdkVersion
)
