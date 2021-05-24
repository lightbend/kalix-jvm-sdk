// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `akkasls-codegen` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(
      commonSettings ++ Seq(
        skip in publish := true
      )
    )
    .aggregate(
      `akkasls-codegen-core`,
      `akkasls-codegen-java`,
      `akkasls-codegen-js`,
      `akkasls-codegen-js-cli`
    )

lazy val `akkasls-codegen-core` =
  project
    .in(file("core"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        library.akkaserverless,
        library.protobufJava,
        library.munit           % Test,
        library.munitScalaCheck % Test
      )
    )

lazy val `akkasls-codegen-java` =
  project
    .in(file("java-gen"))
    .configs(IntegrationTest)
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings, Defaults.itSettings)
    .settings(
      libraryDependencies ++= Seq(
        library.kiama,
        library.commonsIo       % "it,test",
        library.munit           % "it,test",
        library.munitScalaCheck % "it,test",
        library.logback         % "it",
        library.requests        % "it",
        library.testcontainers  % "it",
        library.typesafeConfig  % "it"
      ),
      testOptions in IntegrationTest += Tests.Argument(
        s"-Djava-codegen.jar=${assembly.value}"
      )
    )
    .dependsOn(`akkasls-codegen-core`)

lazy val `akkasls-codegen-js` =
  project
    .in(file("js-gen"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        library.kiama,
        library.commonsIo       % Test,
        library.munit           % Test,
        library.munitScalaCheck % Test
      )
    )
    .dependsOn(`akkasls-codegen-core`)

lazy val cachedNativeImage = taskKey[File]("A cached version of the nativeImage task key, that only rebuilds when required.")

lazy val `akkasls-codegen-js-cli` =
  project
    .in(file("js-gen-cli"))
    .configs(IntegrationTest)
    .enablePlugins(AutomateHeaderPlugin, BuildInfoPlugin, NativeImagePlugin)
    .settings(commonSettings, Defaults.itSettings)
    .settings(
      buildInfoKeys := Seq[BuildInfoKey](version),
      buildInfoPackage := "com.lightbend.akkasls.codegen.js",
      name in NativeImage := "akkasls-codegen-js",
      /**
        * Due to limitations of the Windows command prompt/PowerShell, with a the native-image command fails with a long classpath
        * By using sbt-assembly, we first build a fat JAR which is then able to be used in place of the full classpath
        *
        * This has been raised as an issue against the plugin: https://github.com/scalameta/sbt-native-image/issues/26
        */
      fullClasspath in Compile := Seq(Attributed(assembly.value)(AttributeMap.empty)),
      cachedNativeImage := Def.taskDyn {
        import sbt.util.CacheImplicits._

        val store = streams.value.cacheStoreFactory.make("assembled-jar-info-cache")

        val trackedNativeImage = Tracked.inputChanged[ModifiedFileInfo, Def.Initialize[Task[File]]](store) {
          case (fatJarChanged, _) =>
              if (fatJarChanged) {
                // Assembled fat JAR has changed, rebuild native image
                nativeImage
              } else {
                streams.value.log.info(s"Native image up to date: ${nativeImageOutput.value}")
                nativeImageOutput.toTask
              }
            }

        trackedNativeImage(FileInfo.lastModified(assembly.value))
      }.value,
      nativeImageAgentMerge := true,
      nativeImageOptions ++= Seq(
        "--no-fallback",
        "-H:JNIConfigurationFiles=" + (resourceDirectory in Compile).value / "jni-config.json",
        "-H:DynamicProxyConfigurationFiles=" + (resourceDirectory in Compile).value / "proxy-config.json",
        "-H:ReflectionConfigurationFiles=" + (resourceDirectory in Compile).value / "reflect-config.json",
        "-H:ResourceConfigurationFiles=" + (resourceDirectory in Compile).value / "resource-config.json"
      ),
      libraryDependencies ++= Seq(
        library.scopt,
        library.munit           % "it,test",
        library.munitScalaCheck % "it,test",
        library.logback         % "it",
        library.requests        % "it",
        library.testcontainers  % "it",
        library.typesafeConfig  % "it"
      ),
      testOptions in IntegrationTest += Tests.Argument(
        s"-Djs-codegen-cli.native-image=${cachedNativeImage.value}"
      ),
      skip in publish := true
    )
    .dependsOn(`akkasls-codegen-js`)

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val commonsIo      = "2.8.0"
      val kiama          = "2.4.0"
      val logback        = "1.2.3"
      val munit          = "0.7.20"
      val protobufJava   = "3.13.0"
      val requests       = "0.6.5"
      val scopt          = "4.0.0"
      val testcontainers = "1.15.3"
      val typesafeConfig = "1.4.1"
      val akkaserverless = "0.7.0-beta.8"
    }
    val commonsIo       = "commons-io"                     % "commons-io"       % Version.commonsIo
    val kiama           = "org.bitbucket.inkytonik.kiama" %% "kiama"            % Version.kiama
    val logback         = "ch.qos.logback"                 % "logback-classic"  % Version.logback
    val munit           = "org.scalameta"                 %% "munit"            % Version.munit
    val munitScalaCheck = "org.scalameta"                 %% "munit-scalacheck" % Version.munit
    val protobufJava    = "com.google.protobuf"            % "protobuf-java"    % Version.protobufJava
    val requests        = "com.lihaoyi"                   %% "requests"         % Version.requests
    val scopt           = "com.github.scopt"              %% "scopt"            % Version.scopt
    val testcontainers  = "org.testcontainers"             % "testcontainers"   % Version.testcontainers
    val typesafeConfig  = "com.typesafe"                   % "config"           % Version.typesafeConfig
    val akkaserverless =
      "com.akkaserverless" % "akkaserverless-java-sdk" % Version.akkaserverless
  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val commonSettings =
  Seq(
    scalaVersion := "2.13.4",
    organization := "com.lightbend",
    organizationName := "Lightbend Inc",
    startYear := Some(2021),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-encoding",
      "UTF-8",
      "-Ywarn-unused:imports"
    ),
    testFrameworks += new TestFramework("munit.Framework"),
    scalafmtOnCompile := true,
    Compile / compile / wartremoverWarnings ++= Warts.unsafe,
    headerLicense := Some(
      HeaderLicense.Custom(
        """|Copyright (c) Lightbend Inc. 2021
           |
           |""".stripMargin
      )
    ),
    // Publishing
    publishTo := Some("Cloudsmith API" at "https://maven.cloudsmith.io/lightbend/akkaserverless/"),
    pomIncludeRepository := { x => false },
    credentials += Credentials(
      "Cloudsmith API",
      "maven.cloudsmith.io",
      sys.env.getOrElse("CLOUDSMITH_USER", ""),
      sys.env.getOrElse("CLOUDSMITH_PASS", "")
    ),
    // Assembly
    assemblyMergeStrategy in assembly := {
      case s if s.endsWith(".proto") =>
        MergeStrategy.first
      case "module-info.class" => MergeStrategy.discard
      case s                   => MergeStrategy.defaultMergeStrategy(s)
    }
  )
