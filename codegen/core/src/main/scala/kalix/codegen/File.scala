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

import kalix.codegen.SourceGeneratorUtils.packageAsPath

import java.nio.file.Files
import java.nio.file.Path

case class GeneratedFiles(
    managedFiles: Seq[File],
    unmanagedFiles: Seq[File],
    managedTestFiles: Seq[File],
    unmanagedTestFiles: Seq[File],
    integrationTestFiles: Seq[File]) {

  def write(
      managedSourceDirectory: Path,
      unmanagedSourceDirectory: Path,
      managedTestSourceDirectory: Path,
      unmanagedTestSourceDirectory: Path,
      unmanagedIntegrationTestSourceDirectory: Path): Iterable[Path] =
    managedFiles.map(_.writeToDirectory(managedSourceDirectory, onlyIfMissing = false)) ++
    unmanagedFiles.map(_.writeToDirectory(unmanagedSourceDirectory, onlyIfMissing = true)) ++
    managedTestFiles.map(_.writeToDirectory(managedTestSourceDirectory, onlyIfMissing = false)) ++
    unmanagedTestFiles.map(_.writeToDirectory(unmanagedTestSourceDirectory, onlyIfMissing = true)) ++
    integrationTestFiles.map(_.writeToDirectory(unmanagedIntegrationTestSourceDirectory, onlyIfMissing = true))

  def overwrite(
      managedSourceDirectory: Path,
      unmanagedSourceDirectory: Path,
      managedTestSourceDirectory: Path,
      unmanagedTestSourceDirectory: Path,
      unmanagedIntegrationTestSourceDirectory: Path): Iterable[Path] =
    managedFiles.map(_.writeToDirectory(managedSourceDirectory, onlyIfMissing = false)) ++
    unmanagedFiles.map(_.writeToDirectory(unmanagedSourceDirectory, onlyIfMissing = false)) ++
    managedTestFiles.map(_.writeToDirectory(managedTestSourceDirectory, onlyIfMissing = false)) ++
    unmanagedTestFiles.map(_.writeToDirectory(unmanagedTestSourceDirectory, onlyIfMissing = false)) ++
    integrationTestFiles.map(_.writeToDirectory(unmanagedIntegrationTestSourceDirectory, onlyIfMissing = false))

  def addManaged(file: File): GeneratedFiles =
    copy(managedFiles = managedFiles :+ file)
  def addUnmanaged(file: File): GeneratedFiles =
    copy(unmanagedFiles = unmanagedFiles :+ file)

  def addManagedTest(file: File): GeneratedFiles =
    copy(managedTestFiles = managedTestFiles :+ file)
  def addUnmanagedTest(file: File): GeneratedFiles =
    copy(unmanagedTestFiles = unmanagedTestFiles :+ file)

  def addIntegrationTest(file: File): GeneratedFiles =
    copy(integrationTestFiles = integrationTestFiles :+ file)

  def ++(other: GeneratedFiles): GeneratedFiles =
    GeneratedFiles(
      managedFiles ++ other.managedFiles,
      unmanagedFiles ++ other.unmanagedFiles,
      managedTestFiles ++ other.managedTestFiles,
      unmanagedTestFiles ++ other.unmanagedTestFiles,
      integrationTestFiles ++ other.integrationTestFiles)
}
object GeneratedFiles {
  def Empty: GeneratedFiles = GeneratedFiles(Vector.empty, Vector.empty, Vector.empty, Vector.empty, Vector.empty)
}

case class File(name: String, content: String) {
  def prepend(s: String): File =
    copy(content = s + "\n" + content)

  def writeToDirectory(directory: Path, onlyIfMissing: Boolean): Path = {
    val target = directory.resolve(name)
    if (!onlyIfMissing || !target.toFile.exists()) {
      if (!target.getParent.toFile.exists()) target.getParent.toFile.mkdirs()

      val bytes = content.getBytes("UTF8")
      if (!File.hasSameContents(target, content, bytes))
        Files.write(target, bytes)
    }
    target
  }
}
object File {
  def scala(packageName: String, className: String, content: String): File =
    apply(packageName, className, "scala", content)

  def java(pkg: PackageNaming, className: String, content: String): File =
    apply(pkg.javaPackage, className, "java", content)

  def apply(packageName: String, className: String, fileType: String, content: String): File =
    File(s"${packageAsPath(packageName)}/$className.$fileType", content)

  def hasSameContents(target: Path, contents: String, contentBytes: Array[Byte]): Boolean =
    Files.exists(target) &&
    Files.size(target) == contentBytes.length &&
    // assumes that reading full files into memory is ok
    Files.readString(target) == contents
}
