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

package com.lightbend.akkasls.codegen.java

import com.lightbend.akkasls.codegen
import com.lightbend.akkasls.codegen.DescriptorSet
import com.lightbend.akkasls.codegen.GeneratedFiles
import com.lightbend.akkasls.codegen.ModelBuilder

import java.io.File
import java.nio.file.{ FileVisitOption, Files }
import scala.sys.process._

class ExampleSuite extends munit.FunSuite {
  import ExampleSuite._

  val regenerateAll: Boolean = false

  implicit val codegenLog = new com.lightbend.akkasls.codegen.Log {
    override def debug(message: String): Unit = println(s"[DEBUG] $message")
    override def info(message: String): Unit = println(s"[INFO] $message")
  }
  implicit val fqnExtractor = FullyQualifiedNameExtractor

  val testsDir = BuildInfo.test_resourceDirectory / "tests"

  val tests =
    testsDir
      .walk(f => f.isDirectory && f.listFiles().exists(d => d.isDirectory && d.getName == "proto"))
      .toVector

  tests.foreach { testDirUnresolved =>
    val testDir = testsDir.toPath.resolve(testDirUnresolved.toPath).toFile
    val regenerate = testDir / "regenerate"

    val protoDir = testDir / "proto"
    val protos = protoDir.byName(_.endsWith(".proto"))
    val tmpDesc = File.createTempFile("user", ".desc")
    tmpDesc.deleteOnExit()

    val protoSources = protos.map(_.getAbsolutePath).mkString(" ")
    s"""${BuildInfo.protocExecutable.getAbsolutePath} --include_imports --proto_path=${protoDir.getAbsolutePath} --proto_path=${BuildInfo.protocExternalSourcePath} --proto_path=${BuildInfo.protocExternalIncludePath} --descriptor_set_out=${tmpDesc.getAbsolutePath} $protoSources""".!!

    val fileDescs = DescriptorSet.fileDescriptors(tmpDesc).right.get.right.get
    val model = ModelBuilder.introspectProtobufClasses(fileDescs)

    val files = SourceGenerator.generateFiles(model, "org.example.Main")

    def t(testName: String, dir: String, fileSet: GeneratedFiles => Seq[codegen.File]): Unit =
      test(s"${testDirUnresolved.getPath.replace("/", " / ")} / $testName") {
        checkFiles(testDir / dir, fileSet(files))
      }

    if (regenerateAll || regenerate.exists) {
      test(s"${testDir.getName} / first run: generating target files") {
        import scala.language.postfixOps
        files.write(
          testDir / "generated-managed" toPath,
          testDir / "generated-unmanaged" toPath,
          testDir / "generated-test-managed" toPath,
          testDir / "generated-test-unmanaged" toPath,
          testDir / "generated-integration-unmanaged" toPath)
        regenerate.delete()
      }
    } else {
      t("unmanaged", "generated-unmanaged", _.unmanagedFiles)
      t("managed", "generated-managed", _.managedFiles)
      t("unmanaged test", "generated-test-unmanaged", _.unmanagedTestFiles)
      t("managed test", "generated-test-managed", _.managedTestFiles)
      t("unmanaged integration tests", "generated-integration-managed", _.integrationTestFiles)
    }
  }

  def checkFiles(expectedDir: File, actualGenerated: Seq[codegen.File]): Unit = {
    val actual = actualGenerated.map(g => g.name -> g.content).toMap
    val expectedFiles = expectedDir.walkFiles().toVector
    val expectedNameSet = expectedFiles.map(_.getPath).toSet
    val missing = expectedNameSet.diff(actual.keySet)
    val extra = actual.keySet.diff(expectedNameSet)

    assert(missing.isEmpty, s"Files should have been generated but are missing: [${missing.mkString(", ")}]")
    assert(extra.isEmpty, s"Unexpected files were generated: [${extra.mkString(", ")}]")

    expectedFiles.foreach { f =>
      assertNoDiff(actual(f.getPath), Files.readString((expectedDir / f.getPath).toPath))
    }
  }
}
object ExampleSuite {
  implicit class FileTools(val f: File) extends AnyVal {
    def /(path: String): File = new File(f, path)
    def byName(filter: String => Boolean): Seq[File] = f.listFiles(f => filter(f.getName)).toVector
    import scala.collection.JavaConverters._
    def walkFiles(): Iterator[File] = walk(_.isFile)
    def walk(p: File => Boolean): Iterator[File] =
      if (f.exists())
        Files
          .walk(f.toPath, FileVisitOption.FOLLOW_LINKS)
          .map[File](_.toFile)
          .filter(p(_))
          .map[File] { sub =>
            require(sub.getCanonicalPath.startsWith(f.getCanonicalPath))
            new File(sub.getCanonicalPath.drop(f.getCanonicalPath.size + 1))
          }
          .iterator()
          .asScala
      else Iterator.empty
  }
}
