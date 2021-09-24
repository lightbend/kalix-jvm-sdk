import sbt._
import sbt.Keys._

object Dependencies {
  object AkkaServerless {
    val ProtocolVersionMajor = 0
    val ProtocolVersionMinor = 7
    val FrameworkVersion = "0.7.0-beta.19"
  }

  // changing the Scala version of the Java SDK affects end users
  val ScalaVersion = "2.13.6"
  val ScalaVersionForSbtPlugin = "2.12.14"
  val ScalaVersionForCodegen = Seq(ScalaVersionForSbtPlugin)

  val ProtobufVersion = akka.grpc.gen.BuildInfo.googleProtobufVersion

  val AkkaVersion = "2.6.16"
  val AkkaHttpVersion = "10.2.6" // Note: should at least the Akka HTTP version required by Akka gRPC
  val ScalaTestVersion = "3.2.7"
  val JacksonVersion = "2.11.4" // Akka 2.6.16: 2.11.4, google-http-client-jackson2 1.34.0: 2.10.1
  val DockerBaseImageVersion = "adoptopenjdk/openjdk11:debianslim-jre"
  val LogbackVersion = "1.2.3"
  val LogbackContribVersion = "0.1.5"
  val TestContainersVersion = "1.15.3"
  val JUnitVersion = "4.13.2"
  val JUnitInterfaceVersion = "0.11"
  val JUnitJupiterVersion = "5.7.1"

  val CommonsIoVerison = "2.8.0"
  val MunitVersion = "0.7.20"
  val ScoptVersions = "4.0.0"

  val akkaslsProxyProtocol = "com.akkaserverless" % "akkaserverless-proxy-protocol" % AkkaServerless.FrameworkVersion
  val akkaslsSdkProtocol = "com.akkaserverless" % "akkaserverless-sdk-protocol" % AkkaServerless.FrameworkVersion
  val akkaslsTckProtocol = "com.akkaserverless" % "akkaserverless-tck-protocol" % AkkaServerless.FrameworkVersion

  val commonsIo = "commons-io" % "commons-io" % CommonsIoVerison
  val logback = "ch.qos.logback" % "logback-classic" % LogbackVersion
  val logbackContrib = "ch.qos.logback.contrib" % "logback-json-classic" % LogbackContribVersion

  val protobufJava = "com.google.protobuf" % "protobuf-java" % ProtobufVersion
  val protobufJavaUtil = "com.google.protobuf" % "protobuf-java-util" % ProtobufVersion

  val scopt = "com.github.scopt" %% "scopt" % ScoptVersions
  val jacksonCore = "com.fasterxml.jackson.core" % "jackson-core" % JacksonVersion
  val jacksonAnnotations = "com.fasterxml.jackson.core" % "jackson-annotations" % JacksonVersion
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion
  val jacksonJdk8 = "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % JacksonVersion
  val jacksonJsr310 = "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % JacksonVersion
  val jacksonParameterNames = "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % JacksonVersion
  val jacksonScala = "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion

  val testcontainers = "org.testcontainers" % "testcontainers" % TestContainersVersion
  val scalaTest = "org.scalatest" %% "scalatest" % ScalaTestVersion
  val munit = "org.scalameta" %% "munit" % MunitVersion
  val munitScalaCheck = "org.scalameta" %% "munit-scalacheck" % MunitVersion
  val testContainers = "org.testcontainers" % "testcontainers" % TestContainersVersion
  val junit4 = "junit" % "junit" % JUnitVersion
  val junit5 = "org.junit.jupiter" % "junit-jupiter" % JUnitJupiterVersion

  val scalapbCompilerPlugin = "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion
  val sbtProtoc = "com.thesamet" % "sbt-protoc" % "1.0.0"

  val akkaGrpc = "com.lightbend.akka.grpc" % "sbt-akka-grpc" % "2.1.0"

  private val deps = libraryDependencies

  private val coreDeps = Seq(
    akkaDependency("akka-stream"),
    akkaDependency("akka-slf4j"),
    akkaDependency("akka-discovery"),
    akkaHttpDependency("akka-http"),
    akkaHttpDependency("akka-http-core"),
    akkaHttpDependency("akka-http-spray-json"),
    akkaHttpDependency("akka-http2-support"),
    akkaHttpDependency("akka-parsing"),
    protobufJavaUtil,
    akkaslsProxyProtocol % "protobuf-src",
    akkaslsSdkProtocol % "compile;protobuf-src",
    akkaDependency("akka-testkit") % Test,
    akkaDependency("akka-stream-testkit") % Test,
    akkaHttpDependency("akka-http-testkit") % Test,
    scalaTest % Test,
    logback % "test;provided",
    logbackContrib % Provided,
    jacksonCore,
    jacksonAnnotations,
    jacksonDatabind,
    jacksonJdk8,
    jacksonJsr310,
    jacksonParameterNames)

  val sdkCore = deps ++= coreDeps

  // FIXME
  val sdkJava = sdkCore

  val sdkJavaTestKit = deps ++= Seq(testContainers, junit4 % Provided, junit5 % Provided)

  // FIXME
  val sdkScala = deps ++= coreDeps ++ Seq(jacksonScala)

  val sdkScalaTestKit = deps ++= Seq(
    testContainers,
    logback % "test;provided")

  val tck = deps ++= Seq(
    akkaslsTckProtocol % "protobuf-src",
    "com.akkaserverless" % "akkaserverless-tck-protocol" % AkkaServerless.FrameworkVersion % "protobuf-src",
    "ch.qos.logback" % "logback-classic" % LogbackVersion)

  val codegenCore = deps ++= Seq(
    protobufJava,
    akkaslsSdkProtocol % "compile;protobuf-src",
    logback % Test,
    munit % Test,
    munitScalaCheck % Test)

  val codegenJava = deps ++= Seq(commonsIo, logback % Test, munit % Test, munitScalaCheck % Test)

  val codegenScala = deps ++= Seq(scalapbCompilerPlugin, munit % Test)

  val sbtPlugin = Seq(
    // we depend on it in the settings of the plugin since we set keys of the sbt-protoc plugin
    addSbtPlugin(sbtProtoc),
    addSbtPlugin(akkaGrpc))

  lazy val excludeTheseDependencies: Seq[ExclusionRule] = Seq(
    // exclusion rules can be added here
  )

  def akkaDependency(name: String, excludeThese: ExclusionRule*) =
    ("com.typesafe.akka" %% name % AkkaVersion).excludeAll((excludeTheseDependencies ++ excludeThese): _*)

  def akkaHttpDependency(name: String, excludeThese: ExclusionRule*) =
    ("com.typesafe.akka" %% name % AkkaHttpVersion).excludeAll((excludeTheseDependencies ++ excludeThese): _*)

}
