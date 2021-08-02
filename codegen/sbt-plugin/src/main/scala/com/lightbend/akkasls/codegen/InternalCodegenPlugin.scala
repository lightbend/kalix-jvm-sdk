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

package com.lightbend.akkasls.codegen

import scala.collection.immutable

import sbt.AutoPlugin
import sbt.File
import com.lightbend.akkasls.codegen.java.SourceGenerator
import org.slf4j.LoggerFactory

/**
 * A plugin that allows to use a code generator compiled in one subproject to be used in a test project
 *
 * This plugin is not intended to be published. We use it internally to generate and compile code in the
 * codegen compilation tests
 */
object InternalCodegenPlugin extends AutoPlugin {

  override def trigger = noTrigger

  val logger = LoggerFactory.getLogger(InternalCodegenPlugin.getClass)

  def runAkkaServerlessCodegen(protobufDescriptor: File, destination: File): Seq[File] = {
    val descriptors =
      DescriptorSet.fileDescriptors(protobufDescriptor) match {
        case Right(fileDescriptors) =>
          fileDescriptors.map {
            case Right(file) => file
            case Left(failure) =>
              throw new RuntimeException(
                s"There was a problem building the file descriptor from its protobuf: $failure"
              )
          }
        case Left(failure) =>
          throw new RuntimeException(s"There was a problem opening the protobuf descriptor file ${failure}", failure.e)
      }

    implicit val codegenLog = new Log {
      override def debug(message: String): Unit = logger.debug(message)
      override def info(message: String): Unit = logger.info(message)
      override def warning(message: String): Unit = logger.warn(message)
      override def error(message: String): Unit = logger.error(message)
    }

    val model = ModelBuilder.introspectProtobufClasses(descriptors)
    val path = destination.toPath
    // we generate all files on the same place as we only care if they will compile
    // also, we want all files to be generated in the target folder (at least for now and for this specific case)
    val generatedFiles = SourceGenerator.generate(model, path, path, path, path, "com.example.Main")
    generatedFiles.map(_.toFile).to[immutable.Seq]
  }

}
