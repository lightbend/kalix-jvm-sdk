scalaVersion := "2.13.14"
resolvers += "Akka library repository".at("https://repo.akka.io/maven")
enablePlugins(KalixPlugin)

libraryDependencies ++= Seq("org.scalatest" %% "scalatest" % "3.2.12" % Test)
