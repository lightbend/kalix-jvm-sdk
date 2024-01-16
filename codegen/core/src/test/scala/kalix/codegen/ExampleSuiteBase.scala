/*
 * Copyright 2024 Lightbend Inc.
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

package kalix.codegen

import com.google.protobuf.Descriptors
import java.nio.file.{ FileVisitOption, Files }

import scala.sys.process._

abstract class ExampleSuiteBase extends munit.FunSuite {
  import ExampleSuiteBase._

  def BuildInfo: ExampleSuiteBuildInfo
  def createMessageTypeExtractor(
      fileDescriptors: Seq[Descriptors.FileDescriptor]): ModelBuilder.ProtoMessageTypeExtractor
  def generateFiles(model: ModelBuilder.Model): GeneratedFiles
  def regenerateAll: Boolean

  def propertyPath: String

  def regenerate(testDir: java.io.File): Boolean = regenerateAll || {
    sys.props.get(propertyPath) match {
      case Some("all") => true
      case Some(node)  => testDir.toString.contains(node)
      case None        => false
    }
  }

  implicit val codegenLog = new kalix.codegen.Log {
    override def debug(message: String): Unit = println(s"[DEBUG] $message")
    override def warn(message: String): Unit = println(s"[WARNING] $message")
    override def info(message: String): Unit = println(s"[INFO] $message")
  }
  val testsDir = BuildInfo.test_resourceDirectory / "tests"

  val tests =
    testsDir
      .walkNoAbsPath(f => f.isDirectory && f.listFiles().exists(d => d.isDirectory && d.getName == "proto"))
      .toVector

  tests.foreach { testDirUnresolved =>
    val testDir = testsDir.toPath.resolve(testDirUnresolved.toPath).toFile

    val protoDir = testDir / "proto"
    val protos = protoDir.walk(_.getName.endsWith(".proto"))
    val tmpDesc = java.io.File.createTempFile("user", ".desc")
    tmpDesc.deleteOnExit()

    val protoSources = protos.map(_.getAbsolutePath).mkString(" ")
    s"""${BuildInfo.protocExecutable.getAbsolutePath} --include_imports --proto_path=${protoDir.getAbsolutePath} --proto_path=${BuildInfo.protocExternalSourcePath} --proto_path=${BuildInfo.protocExternalIncludePath} --descriptor_set_out=${tmpDesc.getAbsolutePath} $protoSources""".!!

    val fileDescs = DescriptorSet.fileDescriptors(tmpDesc).right.get.right.get
    implicit val messageTypeExtractor = createMessageTypeExtractor(fileDescs.toSeq)
    val model = ModelBuilder.introspectProtobufClasses(fileDescs)

    val files = generateFiles(model)

    def testFiles(testName: String, dir: String, fileSet: GeneratedFiles => Seq[File]): Unit =
      test(s"${testDirUnresolved.getPath.replace("/", " / ")} / $testName") {
        assertFiles(testDir / dir, fileSet(files))
      }

    if (regenerate(testDir)) {
      test(s"${testDir.getName} / first run: generating target files") {
        import scala.language.postfixOps
        files.overwrite(
          testDir / "generated-managed" toPath,
          testDir / "generated-unmanaged" toPath,
          testDir / "generated-test-managed" toPath,
          testDir / "generated-test-unmanaged" toPath,
          testDir / "generated-integration-unmanaged" toPath)
      }
    } else {
      testFiles("unmanaged", "generated-unmanaged", _.unmanagedFiles)
      testFiles("managed", "generated-managed", _.managedFiles)
      testFiles("unmanaged test", "generated-test-unmanaged", _.unmanagedTestFiles)
      testFiles("managed test", "generated-test-managed", _.managedTestFiles)
      testFiles("unmanaged integration tests", "generated-integration-unmanaged", _.integrationTestFiles)
    }
  }

  def assertFiles(expectedDir: java.io.File, actualGenerated: Seq[File]): Unit = {
    val actual = actualGenerated.map(g => g.name -> g.content).toMap
    val expectedFiles = expectedDir.walkFilesNoAbsPath().toVector
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
object ExampleSuiteBase {
  import java.io.File
  type ExampleSuiteBuildInfo = {
    def protocExecutable: File
    def protocExternalIncludePath: File
    def protocExternalSourcePath: File
    def test_resourceDirectory: File
  }

  implicit class FileTools(val f: File) extends AnyVal {
    def /(path: String): File = new File(f, path)
    import scala.collection.JavaConverters._
    def walk(filter: File => Boolean): Iterator[File] = {
      if (f.exists())
        Files
          .walk(f.toPath, FileVisitOption.FOLLOW_LINKS)
          .map[File](_.toFile)
          .filter(filter(_))
          .iterator()
          .asScala
      else Iterator.empty
    }
    def walkFilesNoAbsPath(): Iterator[File] = walkNoAbsPath(_.isFile)
    def walkNoAbsPath(filter: File => Boolean): Iterator[File] =
      walk(filter)
        .map[File] { sub =>
          require(sub.getCanonicalPath.startsWith(f.getCanonicalPath))
          new File(sub.getCanonicalPath.drop(f.getCanonicalPath.size + 1))
        }
  }
}
