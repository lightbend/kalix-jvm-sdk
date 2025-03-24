import sbt._
import sbt.Keys._

object Dependencies {
  object Kalix {
    val ProtocolVersionMajor = 1
    val ProtocolVersionMinor = 1
    val RuntimeImage = "registry.akka.io/kalix-runtime"
    val RuntimeVersion = System.getProperty(
      "kalix-runtime.version",
      // temporarily accept the old system property name
      System.getProperty("kalix-proxy.version", "fake999.999.111"))
  }

  // changing the Scala version of the Java SDK affects end users
  val ScalaVersion = "2.13.14"
  val ScalaVersionForTooling = "2.12.19"
  val Scala3Version = "3.3.3"
  val CrossScalaVersions = Seq(ScalaVersion, Scala3Version)

  val ProtobufVersion = akka.grpc.gen.BuildInfo.googleProtobufVersion

  val AkkaVersion = "2.10.2"
  val AkkaHttpVersion = "10.7.0" // Note: should at least the Akka HTTP version required by Akka gRPC
  val ScalaTestVersion = "3.2.14"
  // https://github.com/akka/akka/blob/main/project/Dependencies.scala#L31
  val JacksonVersion = "2.15.4"
  val JacksonDatabindVersion = JacksonVersion
  val LogbackVersion = "1.5.17"
  val LogbackContribVersion = "0.1.5"
  val TestContainersVersion = "1.20.5"
  val JUnitVersion = "4.13.2"
  val JUnitInterfaceVersion = "0.11"
  val JUnitJupiterVersion = "5.10.1"
  val OpenTelemetryVersion = "1.39.0"
  val OpenTelemetrySemConv = "1.25.0-alpha"

  val CommonsIoVersion = "2.18.0"
  val MunitVersion = "0.7.29"

  val kalixProxyProtocol = "io.kalix" % "kalix-proxy-protocol" % Kalix.RuntimeVersion
  val kalixSdkProtocol = "io.kalix" % "kalix-sdk-protocol" % Kalix.RuntimeVersion
  val kalixTckProtocol = "io.kalix" % "kalix-tck-protocol" % Kalix.RuntimeVersion
  val kalixTestkitProtocol = "io.kalix" % "kalix-testkit-protocol" % Kalix.RuntimeVersion

  val commonsIo = "commons-io" % "commons-io" % CommonsIoVersion
  val logback = "ch.qos.logback" % "logback-classic" % LogbackVersion
  val logbackJson = "ch.qos.logback.contrib" % "logback-json-classic" % LogbackContribVersion
  val logbackJackson = "ch.qos.logback.contrib" % "logback-jackson" % LogbackContribVersion

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
  val junit5Vintage = "org.junit.vintage" % "junit-vintage-engine" % JUnitJupiterVersion

  val opentelemetryApi = "io.opentelemetry" % "opentelemetry-api" % OpenTelemetryVersion
  val opentelemetrySdk = "io.opentelemetry" % "opentelemetry-sdk" % OpenTelemetryVersion
  val opentelemetryExporterOtlp = "io.opentelemetry" % "opentelemetry-exporter-otlp" % OpenTelemetryVersion
  val opentelemetryContext = "io.opentelemetry" % "opentelemetry-context" % OpenTelemetryVersion
  val opentelemetrySemConv = "io.opentelemetry.semconv" % "opentelemetry-semconv" % OpenTelemetrySemConv

  val scalapbCompilerPlugin = "com.thesamet.scalapb" %% "compilerplugin" % scalapb.compiler.Version.scalapbVersion
  val scalaPbValidateCore = "com.thesamet.scalapb" %% "scalapb-validate-core" % "0.3.4"
  val sbtProtoc = "com.thesamet" % "sbt-protoc" % "1.0.0"

  val akkaGrpc = "com.lightbend.akka.grpc" % "sbt-akka-grpc" % akka.grpc.gen.BuildInfo.version
  val scalaCollectionCompat = "org.scala-lang.modules" %% "scala-collection-compat" % "2.10.0"

  private val deps = libraryDependencies

  private val sdkDeps = Seq(
    akkaDependency("akka-stream"),
    akkaDependency("akka-slf4j"),
    akkaDependency("akka-discovery"),
    akkaDependency("akka-pki"),
    akkaHttpDependency("akka-http"),
    akkaHttpDependency("akka-http-core"),
    akkaHttpDependency("akka-http-spray-json"),
    akkaHttpDependency("akka-parsing"),
    protobufJavaUtil,
    kalixProxyProtocol % "protobuf-src",
    kalixSdkProtocol % "compile;protobuf-src",
    scalaPbValidateCore,
    opentelemetryApi,
    opentelemetrySdk,
    opentelemetryExporterOtlp,
    opentelemetryContext,
    opentelemetrySemConv,
    akkaDependency("akka-testkit") % Test,
    akkaDependency("akka-actor-testkit-typed") % Test,
    akkaDependency("akka-stream-testkit") % Test,
    akkaHttpDependency("akka-http-testkit") % Test,
    scalaTest % Test,
    logback,
    logbackJson,
    logbackJackson,
    jacksonCore,
    jacksonAnnotations,
    jacksonDatabind,
    jacksonJdk8,
    jacksonJsr310,
    jacksonParameterNames)

  val devTools = deps ++= Seq(scalaCollectionCompat, "com.typesafe" % "config" % "1.4.2", scalaTest % Test)

  val javaSdk = deps ++= sdkDeps

  val javaSdkTestKit = deps ++= Seq(
    testContainers,
    junit4,
    junit5,
    junit5Vintage,
    scalaTest % Test,
    kalixTestkitProtocol % "protobuf-src",
    scalapbCompilerPlugin,
    akkaDependency("akka-testkit"),
    akkaDependency("akka-actor-testkit-typed") % Test)

  val scalaSdk = deps ++= sdkDeps ++ Seq(jacksonScala)

  val scalaSdkTestKit = deps ++= Seq(testContainers, logback % "test;provided", scalaTest % Test)

  val tck = deps ++= Seq(
    // FIXME: For now TCK protos have been copied and adapted into this project.
    //        Running the TCK is still meaningful as it runs the TCK check against the defined framework version.
    //        Eventually, the final form of protos from should be backported to the framework.
    //        See https://github.com/lightbend/kalix-jvm-sdk/issues/605
    //  kalixTckProtocol % "protobuf-src",
    //  "io.kalix" % "kalix-tck-protocol" % Kalix.RuntimeVersion % "protobuf-src",
    logback)

  val codegenCore = deps ++= Seq(
    protobufJava,
    scalaCollectionCompat,
    "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
    "com.google.guava" % "guava" % "30.1-jre",
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
