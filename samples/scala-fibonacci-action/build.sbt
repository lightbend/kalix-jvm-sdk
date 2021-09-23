name := "fibonacci-action"

organization := "com.akkaseverless.samples"
organizationHomepage := Some(url("https://akkaserverless.com"))
licenses := Seq(
  ("CC0", url("https://creativecommons.org/publicdomain/zero/1.0"))
)

scalaVersion := "2.13.6"

enablePlugins(AkkaserverlessPlugin)

Compile / scalacOptions ++= Seq(
  "-target:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint"
)
Compile / javacOptions ++= Seq(
  "-Xlint:unchecked",
  "-Xlint:deprecation",
  "-parameters" // for Jackson
)

Test / parallelExecution := false
Test / testOptions += Tests.Argument("-oDF")
Test / logBuffered := false

run / fork := false
Global / cancelable := false // ctrl-c
