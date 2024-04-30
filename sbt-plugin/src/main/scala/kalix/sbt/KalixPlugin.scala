/*
 * Copyright (C) 2021-2024 Lightbend Inc. <https://www.lightbend.com>
 */

package kalix.sbt

import akka.grpc.sbt.AkkaGrpcPlugin
import akka.grpc.sbt.AkkaGrpcPlugin.autoImport.{ akkaGrpcCodeGeneratorSettings, akkaGrpcGeneratedSources }
import akka.grpc.sbt.AkkaGrpcPlugin.autoImport.AkkaGrpc
import java.lang.{ Boolean => JBoolean }
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

import kalix.codegen.scalasdk.{ gen, genTests, genUnmanaged, genUnmanagedTest, BuildInfo, KalixGenerator }
import sbt.{ Compile, _ }
import sbt.Keys._
import sbtprotoc.ProtocPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB
import sbt.complete.Parsers.spaceDelimited

object KalixPlugin extends AutoPlugin {
  override def trigger = noTrigger
  override def requires = ProtocPlugin && AkkaGrpcPlugin

  trait Keys { _: autoImport.type =>
    val generateUnmanaged = taskKey[Seq[File]](
      "Generate \"unmanaged\" kalix scaffolding code based on the available .proto definitions.\n" +
      "These are the source files that are placed in the source tree, and after initial generation should typically be maintained by the user.\n" +
      "Files that already exist they are not re-generated.")
    val temporaryUnmanagedDirectory = settingKey[File]("Directory to generate 'unmanaged' sources into")
    val onlyUnitTest = settingKey[Boolean]("Filters out integration tests. By default: false")
    val protobufDescriptorSetOut = settingKey[File]("The file to write the descriptor set to")
    val rootPackage = settingKey[Option[String]](
      "A root scala package to use for generated common classes such as Main, by default auto detected from protobuf files")

    val runAll = inputKey[Unit]("Run all")
  }

  object autoImport extends Keys
  import autoImport._

  val KalixSdkVersion = BuildInfo.version
  val KalixProtocolVersion = BuildInfo.protocolVersion
  val AkkaVersion = BuildInfo.akkaVersion

  private val dockerComposeKey = "-Dkalix.dev-mode.docker-compose-file="
  private val disableDockerCompose = dockerComposeKey + "none"

  // kalix properties eventually passed by user
  private val passedKalixArgs =
    sys.props.collect {
      case (key, value) if key.startsWith("kalix") => s"-D$key=$value"
    }.toSeq

  private val defaultJvmArgs: Seq[String] = Seq("-Dkalix.user-function-interface=0.0.0.0")

  override def projectSettings =
    Def.settings(
      libraryDependencies ++= Seq(
        "io.kalix" % "kalix-sdk-protocol" % KalixProtocolVersion % "protobuf-src",
        "com.google.protobuf" % "protobuf-java" % "3.17.3" % "protobuf",
        "io.kalix" %% "kalix-scala-sdk-protobuf-testkit" % KalixSdkVersion % Test),

      // -------------------------------------------------------------------------------------------
      // run task
      Compile / run / fork := true,
      // pass Jvm Args to forked process and disable docker-compose
      Compile / run / javaOptions ++= disableDockerCompose +: (defaultJvmArgs ++ passedKalixArgs),
      // redefine run task in other to be able to print the warning below
      Compile / run := Def.inputTaskDyn {
        val logger = streams.value.log
        logger.warn("Kalix Runtime won't start.")
        logger.warn("--------------------------------------------------------------------------------------")
        logger.warn("To test this application locally you should either run it using 'sbt runAll'")
        logger.warn("or start the Kalix Runtime by hand using the provided docker-compose file.")
        logger.warn("--------------------------------------------------------------------------------------")

        val userArgs = {
          val args = spaceDelimited("<args>").parsed
          " " + args.mkString(" ")
        }

        Def.taskDyn {
          Defaults
            .runTask(Runtime / fullClasspath, Compile / run / mainClass, Compile / run / runner)
            .toTask(userArgs)
        }
      }.evaluated,

      // -------------------------------------------------------------------------------------------
      // runAll task
      Compile / runAll / fork := true,
      // pass Jvm Args to forked process with default docker-compose settings
      Compile / runAll / javaOptions ++= defaultJvmArgs ++ passedKalixArgs,

      // workaround for https://github.com/sbt/sbt/issues/4038
      Compile / runAll / forkOptions := {
        val forkOpts = (Compile / run / forkOptions).value
        forkOpts.withRunJVMOptions((Compile / runAll / javaOptions).value.toVector)
      },
      // redefine the runner for runAll
      // this is needed in order to have the forkOptions applied
      // this is a simplified clone of Defaults.runnerInit
      Compile / runAll / runner := {
        val forked = (Compile / runAll / fork).value
        val opts = (Compile / runAll / forkOptions).value
        val logger = streams.value.log
        if (forked) {
          logger.debug(s"javaOptions: ${opts.runJVMOptions}")
          new ForkRun(opts)
        } else {
          val tmp = taskTemporaryDirectory.value
          val si = scalaInstance.value
          val trap = trapExit.value
          logger.warn(s"${opts.runJVMOptions.mkString(",")} will be ignored, (Compile / runAll / fork) is set to false")
          new Run(si, trap, tmp)
        }
      },
      Compile / runAll := Def.inputTaskDyn {
        val logger = streams.value.log
        logger.info("Kalix Runtime container will start in the background")
        logger.info("--------------------------------------------------------------------------------------")

        val userArgs = {
          val args = spaceDelimited("<args>").parsed
          " " + args.mkString(" ")
        }

        Def.taskDyn {
          Defaults
            .runTask(Runtime / fullClasspath, Compile / run / mainClass, Compile / runAll / runner)
            .toTask(userArgs)
        }
      }.evaluated,

      // -------------------------------------------------------------------------------------------
      // below are the settings configuring gRPC compilation
      Compile / PB.targets +=
        gen(
          (akkaGrpcCodeGeneratorSettings.value :+ KalixGenerator.enableDebug)
          ++ rootPackage.value.map(KalixGenerator.rootPackage)) -> (Compile / sourceManaged).value,
      Compile / temporaryUnmanagedDirectory := (Compile / crossTarget).value / "kalix-unmanaged",
      Test / temporaryUnmanagedDirectory := (Test / crossTarget).value / "kalix-unmanaged-test",
      protobufDescriptorSetOut := (Compile / resourceManaged).value / "protobuf" / "descriptor-sets" / "user-function.desc",
      rootPackage := None,
      // FIXME there is a name clash between the Akka gRPC server-side service 'handler'
      // and the Kalix 'handler'. For now working around it by only generating
      // the client, but we should probably resolve this before the first public release.
      Compile / akkaGrpcGeneratedSources := Seq(AkkaGrpc.Client),
      Compile / PB.targets ++= Seq(
        genUnmanaged((akkaGrpcCodeGeneratorSettings.value :+ KalixGenerator.enableDebug)
        ++ rootPackage.value.map(KalixGenerator.rootPackage)) -> (Compile / temporaryUnmanagedDirectory).value),
      Compile / PB.generate := (Compile / PB.generate)
        .dependsOn(Def.task {
          protobufDescriptorSetOut.value.getParentFile.mkdirs()
        })
        .value,
      Compile / PB.protocOptions ++= Seq(
        "--descriptor_set_out",
        protobufDescriptorSetOut.value.getAbsolutePath,
        "--include_source_info"),
      Test / PB.targets ++= Seq(
        genUnmanagedTest((akkaGrpcCodeGeneratorSettings.value :+ KalixGenerator.enableDebug)
        ++ rootPackage.value.map(KalixGenerator.rootPackage)) -> (Test / temporaryUnmanagedDirectory).value),
      Test / PB.protoSources ++= (Compile / PB.protoSources).value,
      Test / PB.targets +=
        genTests(
          (akkaGrpcCodeGeneratorSettings.value :+ KalixGenerator.enableDebug)
          ++ rootPackage.value.map(KalixGenerator.rootPackage)) -> (Test / sourceManaged).value,
      Compile / generateUnmanaged := {
        Files.createDirectories(Paths.get((Compile / temporaryUnmanagedDirectory).value.toURI))
        // Make sure generation has happened
        val _ = (Compile / PB.generate).value
        // Then copy over any new generated unmanaged sources
        copyIfNotExist(
          Paths.get((Compile / temporaryUnmanagedDirectory).value.toURI),
          Paths.get((Compile / sourceDirectory).value.toURI).resolve("scala"))
      },
      Test / generateUnmanaged := {
        Files.createDirectories(Paths.get((Test / temporaryUnmanagedDirectory).value.toURI))
        // Make sure generation has happened
        val _ = (Test / PB.generate).value
        // Then copy over any new generated unmanaged sources
        copyIfNotExist(
          Paths.get((Test / temporaryUnmanagedDirectory).value.toURI),
          Paths.get((Test / sourceDirectory).value.toURI).resolve("scala"))
      },
      Compile / managedSources :=
        (Compile / managedSources).value.filter(s => !isIn(s, (Compile / temporaryUnmanagedDirectory).value)),
      Compile / managedResources += {
        // make sure the file has been generated
        val _ = (Compile / PB.generate).value
        protobufDescriptorSetOut.value
      },
      Compile / unmanagedSources := {
        val _ = (Test / unmanagedSources).value // touch unmanaged test sources, so that they are generated on `compile`
        (Compile / generateUnmanaged).value ++ (Compile / unmanagedSources).value
      },
      Test / managedSources :=
        (Test / managedSources).value.filter(s => !isIn(s, (Test / temporaryUnmanagedDirectory).value)),
      Test / unmanagedSources :=
        (Test / generateUnmanaged).value ++ (Test / unmanagedSources).value,
      onlyUnitTest := {
        sys.props.get("onlyUnitTest") match {
          case Some("") => true
          case Some(x)  => JBoolean.valueOf(x)
          case None     => false
        }
      },
      Test / testOptions ++= {
        if (onlyUnitTest.value)
          Seq(Tests.Filter(name => !name.endsWith("IntegrationSpec")))
        else Nil
      })

  def isIn(file: File, dir: File): Boolean =
    Paths.get(file.toURI).startsWith(Paths.get(dir.toURI))

  private def copyIfNotExist(from: Path, to: Path): Seq[File] = {
    Files
      .walk(from)
      .filter(Files.isRegularFile(_))
      .flatMap[File](file => {
        val target = to.resolve(from.relativize(file))
        if (!Files.exists(target)) {
          Files.createDirectories(target.getParent)
          Files.copy(file, target)
          java.util.stream.Stream.of[File](target.toFile)
        } else java.util.stream.Stream.empty()
      })
      .toArray(new Array[File](_))
      .toVector
  }

}
