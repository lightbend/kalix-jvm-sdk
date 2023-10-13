import net.aichler.jupiter.sbt.Import.JupiterKeys
import sbt._
import sbt.Keys._

object Dependencies {
  object Kalix {
    val ProtocolVersionMajor = 1
    val ProtocolVersionMinor = 1
    val ProxyVersion = System.getProperty("kalix-proxy.version", "1.1.19")
  }

  // changing the Scala version of the Java SDK affects end users
  val ScalaVersion = "2.13.10"
  val ScalaVersionForTooling = "2.12.15"

  val ProtobufVersion = // akka.grpc.gen.BuildInfo.googleProtobufVersion
    "3.21.12" // explicitly overriding the 3.21.1 version from Akka gRPC 2.1.6 (even though its build says 3.20.1)

  val AkkaVersion = "2.6.21"
  val AkkaHttpVersion = "10.2.10" // Note: should at least the Akka HTTP version required by Akka gRPC
  val ScalaTestVersion = "3.2.14"
  val JacksonVersion = "2.14.3"
  val JacksonDatabindVersion = "2.14.3"
  val DockerBaseImageVersion = "adoptopenjdk/openjdk11:debianslim-jre"
  val LogbackVersion = "1.4.5"
  val LogbackContribVersion = "0.1.5"
  val TestContainersVersion = "1.17.6"
  val JUnitVersion = "4.13.2"
  val JUnitInterfaceVersion = "0.11"
  val JUnitJupiterVersion = "5.7.1"
  val SpringFrameworkVersion = "6.0.9"
  // make sure to sync spring-boot-starter-parent version in samples and archetype to this version
  val SpringBootVersion = "3.1.0"

  val CommonsIoVersion = "2.11.0"
  val MunitVersion = "0.7.29"

  val kalixProxyProtocol = "io.kalix" % "kalix-proxy-protocol" % Kalix.ProxyVersion
  val kalixSdkProtocol = "io.kalix" % "kalix-sdk-protocol" % Kalix.ProxyVersion
  val kalixTckProtocol = "io.kalix" % "kalix-tck-protocol" % Kalix.ProxyVersion
  val kalixTestkitProtocol = "io.kalix" % "kalix-testkit-protocol" % Kalix.ProxyVersion

  val commonsIo = "commons-io" % "commons-io" % CommonsIoVersion
  val logback = "ch.qos.logback" % "logback-classic" % LogbackVersion
  val logbackJson = "ch.qos.logback.contrib" % "logback-json-classic" % LogbackContribVersion
  val logbackJackson = "ch.qos.logback.contrib" % "logback-jackson" % LogbackContribVersion

  // akka-slf4j pulls in slf4j-api v1.7.36 and but we want v2.0.6
  // because of Logback v1.4.5 and because of Spring 3. Therefore we have to explicitly bump slf4j-api to v2.0.6.
  // Version 2.0.6 is also problematic for Akka, but only when using the BehaviorTestKit which is not used in the SDK
  val slf4jApi = "org.slf4j" % "slf4j-api" % "2.0.6"

  val protobufJava = "com.google.protobuf" % "protobuf-java" % ProtobufVersion
  val protobufJavaUtil = "com.google.protobuf" % "protobuf-java-util" % ProtobufVersion

  val jacksonCore = "com.fasterxml.jackson.core" % "jackson-core" % JacksonVersion
  val jacksonAnnotations = "com.fasterxml.jackson.core" % "jackson-annotations" % JacksonVersion
  val jacksonDatabind = "com.fasterxml.jackson.core" % "jackson-databind" % JacksonDatabindVersion
  val jacksonJdk8 = "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % JacksonVersion
  val jacksonJsr310 = "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % JacksonVersion
  val jacksonParameterNames = "com.fasterxml.jackson.module" % "jackson-module-parameter-names" % JacksonVersion
  val jacksonScala = "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion
  val jacksonDataFormatProto = "com.fasterxml.jackson.dataformat" % "jackson-dataformat-protobuf" % JacksonVersion

  val scalaTest = "org.scalatest" %% "scalatest" % ScalaTestVersion
  val munit = "org.scalameta" %% "munit" % MunitVersion
  val munitScalaCheck = "org.scalameta" %% "munit-scalacheck" % MunitVersion
  val testContainers = "org.testcontainers" % "testcontainers" % TestContainersVersion
  val junit4 = "junit" % "junit" % JUnitVersion
  val junit5 = "org.junit.jupiter" % "junit-jupiter" % JUnitJupiterVersion

  val scalapbCompilerPlugin = "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion
  val sbtProtoc = "com.thesamet" % "sbt-protoc" % "1.0.0"

  val akkaGrpc = "com.lightbend.akka.grpc" % "sbt-akka-grpc" % akka.grpc.gen.BuildInfo.version

  private val deps = libraryDependencies

  private val sdkDeps = Seq(
    akkaDependency("akka-stream"),
    akkaDependency("akka-slf4j"),
    akkaDependency("akka-discovery"),
    akkaHttpDependency("akka-http"),
    akkaHttpDependency("akka-http-core"),
    akkaHttpDependency("akka-http-spray-json"),
    akkaHttpDependency("akka-http2-support"),
    akkaHttpDependency("akka-parsing"),
    protobufJavaUtil,
    kalixProxyProtocol % "protobuf-src",
    kalixSdkProtocol % "compile;protobuf-src",
    akkaDependency("akka-testkit") % Test,
    akkaDependency("akka-actor-testkit-typed") % Test,
    akkaDependency("akka-stream-testkit") % Test,
    akkaHttpDependency("akka-http-testkit") % Test,
    scalaTest % Test,
    slf4jApi,
    logback,
    logbackJson,
    logbackJackson,
    jacksonCore,
    jacksonAnnotations,
    jacksonDatabind,
    jacksonJdk8,
    jacksonJsr310,
    jacksonParameterNames)

  val devTools = deps ++= Seq(
    "org.scala-lang.modules" %% "scala-collection-compat" % "2.10.0",
    "com.typesafe" % "config" % "1.4.2",
    scalaTest % Test)

  val javaSdk = deps ++= sdkDeps

  val javaSdkTestKit = deps ++= Seq(
    testContainers,
    // Override for security vulnerabilities. Can be removed once testcontainers is updated:
    // https://github.com/testcontainers/testcontainers-java/issues/4308
    "org.apache.commons" % "commons-compress" % "1.21",
    junit4 % Provided,
    junit5 % Provided,
    scalaTest % Test,
    kalixTestkitProtocol % "protobuf-src",
    scalapbCompilerPlugin,
    akkaDependency("akka-testkit"),
    akkaDependency("akka-actor-testkit-typed") % Test)

  val springDeps = Seq(
    jacksonDataFormatProto,
    "org.springframework" % "spring-web" % SpringFrameworkVersion,
    "org.springframework" % "spring-webflux" % SpringFrameworkVersion,
    "org.springframework" % "spring-webmvc" % SpringFrameworkVersion,
    "org.springframework.boot" % "spring-boot" % SpringBootVersion,
    "org.springframework.boot" % "spring-boot-starter" % SpringBootVersion,
    "org.springframework.boot" % "spring-boot-starter-json" % SpringBootVersion,
    "org.springframework.boot" % "spring-boot-starter-reactor-netty" % SpringBootVersion,
    "jakarta.websocket" % "jakarta.websocket-api" % "2.0.0")

  val javaSdkSpring = deps ++= sdkDeps ++ springDeps ++ Seq(
    "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % IntegrationTest,
    "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
    "org.springframework.boot" % "spring-boot-starter-test" % SpringBootVersion % IntegrationTest,
    junit5 % IntegrationTest,
    junit5 % Test,
    "org.assertj" % "assertj-core" % "3.24.2" % IntegrationTest,
    "org.assertj" % "assertj-core" % "3.24.2" % Test,
    "org.awaitility" % "awaitility" % "4.2.0" % IntegrationTest)

  val javaSdkSpringTestKit =
    deps ++= springDeps ++
    Seq(
      junit5 % Test,
      "net.aichler" % "jupiter-interface" % JupiterKeys.jupiterVersion.value % Test,
      "org.springframework.boot" % "spring-boot-starter-test" % SpringBootVersion)

  // FIXME
  val scalaSdk = deps ++= sdkDeps ++ Seq(jacksonScala)

  val scalaSdkTestKit = deps ++= Seq(testContainers, logback % "test;provided", scalaTest % Test)

  val tck = deps ++= Seq(
    // FIXME: For now TCK protos have been copied and adapted into this project.
    //        Running the TCK is still meaningful as it runs the TCK check against the defined framework version.
    //        Eventually, the final form of protos from should be backported to the framework.
    //        See https://github.com/lightbend/kalix-jvm-sdk/issues/605
    //  kalixTckProtocol % "protobuf-src",
    //  "io.kalix" % "kalix-tck-protocol" % Kalix.ProxyVersion % "protobuf-src",
    logback)

  val codegenCore = deps ++= Seq(
    protobufJava,
    kalixSdkProtocol % "compile;protobuf-src",
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
