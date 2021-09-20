/*
 * Copyright 2021 Lightbend Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.akkaserverless.sbt

import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

import com.akkaserverless.codegen.scalasdk.{ gen, genTests, genUnmanaged, AkkaserverlessGenerator }
import sbt.{ Compile, _ }
import sbt.Keys._
import sbtprotoc.ProtocPlugin
import sbtprotoc.ProtocPlugin.autoImport.PB
import scalapb.GeneratorOption

object AkkaserverlessPlugin extends AutoPlugin {
  override def trigger = allRequirements
  override def requires = ProtocPlugin

  trait Keys { _: autoImport.type =>
    val generateUnmanaged = taskKey[Unit](
      "Generate \"unmanaged\" akkaserverless scaffolding code based on the available .proto definitions.\n" +
      "These are the source files that are placed in the source tree, and after initial generation should typically be maintained by the user.\n" +
      "Files that already exist they are not re-generated.")
    val temporaryUnmanagedDirectory = settingKey[File]("Directory to generate 'unmanaged' sources into")
  }
  object autoImport extends Keys
  import autoImport._

  override def projectSettings: Seq[sbt.Setting[_]] = Seq(
    libraryDependencies ++= Seq(
      "com.akkaserverless" % "akkaserverless-sdk-protocol" % "0.7.0-beta.19" % "protobuf",
      "com.google.protobuf" % "protobuf-java" % "3.17.3" % "protobuf"),
    Compile / PB.targets +=
      gen(Seq(AkkaserverlessGenerator.enableDebug)) -> (Compile / sourceManaged).value / "akkaserverless",
    Compile / temporaryUnmanagedDirectory := (Compile / baseDirectory).value / "target" / "akkaserverless-unmanaged",
    Compile / PB.targets ++= Seq(
      // TODO allow user to customize options
      scalapb.gen(grpc = false) -> (Compile / sourceManaged).value / "scalapb",
      genUnmanaged(Seq(AkkaserverlessGenerator.enableDebug)) -> (Compile / temporaryUnmanagedDirectory).value),
    Test / PB.protoSources ++= (Compile / PB.protoSources).value,
    Test / PB.targets +=
      genTests(Seq(AkkaserverlessGenerator.enableDebug)) -> (Test / sourceManaged).value / "akkaserverless",
    Compile / generateUnmanaged := {
      // Make sure generation has happened
      (Compile / PB.generate).value
      // Then copy over any new generated unmanaged sources
      copyIfNotExist(
        java.nio.file.Paths.get((Compile / temporaryUnmanagedDirectory).value.toURI),
        Paths.get((Compile / sourceDirectory).value.toURI).resolve("scala"))
    },
    Compile / managedSources :=
      (Compile / managedSources).value.filter(s => !isIn(s, (Compile / temporaryUnmanagedDirectory).value)))

  def isIn(file: File, dir: File): Boolean =
    Paths.get(file.toURI).startsWith(Paths.get(dir.toURI))

  private def copyIfNotExist(from: Path, to: Path): Unit = {
    java.nio.file.Files
      .walk(from)
      .filter(Files.isRegularFile(_))
      .forEach(file => {
        val target = to.resolve(from.relativize(file))
        if (!Files.exists(target)) {
          Files.createDirectories(target.getParent)
          Files.copy(file, target)
        }
      })
  }
}
