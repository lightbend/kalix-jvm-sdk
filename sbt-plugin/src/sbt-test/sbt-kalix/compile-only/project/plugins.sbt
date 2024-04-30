resolvers += "Akka library repository".at("https://repo.akka.io/maven")
sys.props.get("plugin.version") match {
  case Some(x) => addSbtPlugin("io.kalix" % "sbt-kalix" % x)
  case _ => sys.error("""|The system property 'plugin.version' is not defined.
                         |Specify this property using the scriptedLaunchOpts -D.""".stripMargin)
}
addSbtPlugin("com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.1.4") // FIXME should be included via sbt-kalix
