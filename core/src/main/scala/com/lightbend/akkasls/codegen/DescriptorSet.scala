/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen

import com.google.protobuf.{ DescriptorProtos, Descriptors }

import java.io.{ File, FileInputStream, FileNotFoundException, IOException }
import java.util.logging.{ Level, Logger }
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Using }
import com.google.protobuf.ExtensionRegistry

/**
  * Provides conveniences for reading and parsing Protobuf descriptor sets
  */
object DescriptorSet {

  /**
    * The descriptor file cannot be opened
    */
  final case class CannotOpen(e: Throwable)

  /**
    * Various error conditions during a read
    */
  sealed abstract class ReadFailure
  case class CannotRead(e: IOException)                                   extends ReadFailure
  case class CannotValidate(e: Descriptors.DescriptorValidationException) extends ReadFailure

  /**
    * Read Protobuf FileDescriptor objects from given a file hosting a DescriptorSet
    * @param file the file to read
    * @return a collection of FileDescriptor objects or an error condition
    */
  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  def fileDescriptors(
      file: File
  ): Either[CannotOpen, Iterable[Either[ReadFailure, Descriptors.FileDescriptor]]] =
    Using[FileInputStream, Either[CannotOpen, Iterable[
      Either[ReadFailure, Descriptors.FileDescriptor]
    ]]](
      new FileInputStream(file)
    ) { fis =>
      val registry = ExtensionRegistry.newInstance()
      registry.add(com.akkaserverless.Annotations.service)
      registry.add(com.akkaserverless.Annotations.file)

      Right(try {
        val descriptorProtos =
          DescriptorProtos.FileDescriptorSet.parseFrom(fis, registry).getFileList.asScala

        for (descriptorProto <- descriptorProtos)
          yield try Right(Descriptors.FileDescriptor.buildFrom(descriptorProto, Array.empty, true))
          catch {
            case e: Descriptors.DescriptorValidationException =>
              Left(CannotValidate(e))
          }
      } catch {
        case e: IOException =>
          List(Left(CannotRead(e)))
      })
    } match {
      case Success(result)                   => result
      case Failure(e: FileNotFoundException) => Left(CannotOpen(e))
      case Failure(e)                        => throw e
    }

  private val descriptorslogger = Logger.getLogger(classOf[Descriptors].getName)
  descriptorslogger.setLevel(Level.OFF); // Silence protobuf

}
