scalaVersion := "2.13.10"

enablePlugins(KalixPlugin)

testOptions ++=(
  // Skip integration tests in CircleCI since to be able to connect to docker
  // we'd have to switch to the 'machine' executor
  if (sys.env.get("CIRCLECI").contains("true"))
    Seq(Tests.Filter(name => !name.endsWith("IntegrationSpec")))
  else
    Nil
)

libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.2.12" % Test)
