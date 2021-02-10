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
    .aggregate(`akkasls-codegen-core`, `akkasls-codegen-java`)

lazy val `akkasls-codegen-core` =
  project
    .in(file("core"))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        library.protobufJava,
        library.munit           % Test,
        library.munitScalaCheck % Test
      )
    )

lazy val `akkasls-codegen-java` =
  project
    .in(file("java-gen"))
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

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {
    object Version {
      val commonsIo    = "2.8.0"
      val kiama        = "2.4.0"
      val munit        = "0.7.20"
      val protobufJava = "3.11.4"
    }
    val commonsIo       = "commons-io"                     % "commons-io"       % Version.commonsIo
    val kiama           = "org.bitbucket.inkytonik.kiama" %% "kiama"            % Version.kiama
    val munit           = "org.scalameta"                 %% "munit"            % Version.munit
    val munitScalaCheck = "org.scalameta"                 %% "munit-scalacheck" % Version.munit
    val protobufJava    = "com.google.protobuf"            % "protobuf-java"    % Version.protobufJava
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
    bintrayOmitLicense := true,
    bintrayOrganization := Some("lightbend"),
    bintrayRepository := "akkaserverless",
    publishMavenStyle := true
  )
