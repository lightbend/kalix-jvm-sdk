/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen

import java.nio.file.Path
import javax.tools.ToolProvider
import java.nio.file.Files
import java.util.stream.Collectors

import scala.jdk.CollectionConverters._
import scala.util.Using
import java.net.URLClassLoader
import com.google.protobuf.Descriptors
import scala.util.control.NonFatal

/**
  * Builds a model of entities and their properties from compiled Java protobuf files.
  */
object ModelBuilder {

  private final val JAVA_SOURCE = ".java"
  private final val JAVA_CLASS  = ".class"

  /**
    * Given a source directory containing protobuf Java source files,
    * return a collection of their paths at any depth.
    *
    * @param protoSourceDirectory the directory to read .java files from
    * @return the collection of java protobuf source files
    */
  def collectProtobufSources(protoSourceDirectory: Path): Iterable[Path] =
    Files
      .walk(protoSourceDirectory)
      .filter(p => Files.isRegularFile(p) && p.toString().endsWith(JAVA_SOURCE))
      .collect(Collectors.toList())
      .asScala

  /**
    * Given a collection of source files and a root from which they can be relativized,
    * return their corresponding class file paths in relation to an output file directory.
    * @param protoSourceDirectory the root directory of all protobuf java sources
    * @param protoSources the full paths of the protobuf java sources
    * @param outputDirectory the directory where the class files should exist
    * @return a collection of paths correlating class files with their source files
    */
  def mapProtobufClasses(
      protoSourceDirectory: Path,
      protoSources: Iterable[Path],
      outputDirectory: Path
  ): Iterable[Path] =
    protoSources
      .map(protoSourceDirectory.relativize)
      .map { entry =>
        val relativeClassEntry = entry
          .resolveSibling(entry.getFileName().toString().replace(JAVA_SOURCE, JAVA_CLASS))
        outputDirectory.resolve(relativeClassEntry)
      }

  /**
    * Given both protobuf sources and their classes, return a new collection of sources
    * that either have no corresponding class or they have been modified more recently.
    * Both the source and class collection items must correlate with each other and the
    * collections must therefore be of the same size.
    *
    * @param protoSources the collection of protobuf sources
    * @param protoClasses the corresponding target protobuf classes, which may or may not exist
    * @return a filtered down collection of sources more recent than any existing corresponding class
    */
  def filterNewProtobufSources(
      protoSources: Iterable[Path],
      protoClasses: Iterable[Path]
  ): Iterable[Path] = {
    assert(protoSources.size == protoClasses.size)
    val distinctProtoClasses = protoClasses.toArray
    protoSources.zipWithIndex
      .filter { case (source, i) =>
        val sourceFile = source.toFile()
        val classFile  = distinctProtoClasses(i).toFile()
        !classFile.exists() || sourceFile.lastModified() > classFile.lastModified()
      }
      .map(_._1)
  }

  /**
    * Compile protobuf Java source files using the Java compiler
    *
    * @param protoSources the sources to compile
    * @param outputDirectory the directory to write .class files to
    * @return 0 for success, non-zero for failure (as per the Java compiler)
    */
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  def compileProtobufSources(protoSources: Iterable[Path], outputDirectory: Path): Int = {
    def jarPath[A](aClass: Class[A]): String =
      aClass.getProtectionDomain().getCodeSource().getLocation().getPath().toString()

    val args = Array(
      "-d",
      outputDirectory.toString(),
      "-cp",
      s"${jarPath(classOf[com.google.protobuf.Descriptors.Descriptor])}:" +
      s"${jarPath(classOf[io.cloudstate.EntityKey])}"
    ) ++ protoSources.map(_.toString())

    val _ = outputDirectory.toFile().mkdir()

    val compiler = ToolProvider.getSystemJavaCompiler()
    compiler.run(null, null, null, args: _*)
  }

  /**
    * An entity represents the primary model object and is conceptually equivalent to a class, or a type of state.
    * An entity will have multiple Entity instances of it which can handle commands. For example, a user function may
    * implement a chat room entity, encompassing the logic associated with chat rooms, and a particular chat room may
    * be an instance of that entity, containing a list of the users currently in the room and a history of the messages
    * sent to it. Each entity has a particular Entity type, which defines how the entity’s state is persisted, shared,
    * and what its capabilities are.
    */
  sealed abstract class Entity

  /**
    * A type of Entity that stores its state using a journal of events, and restores its state
    * by replaying that journal.
    */
  case class EventSourcedEntity(fullName: String) extends Entity

  /**
    * Given a collection of classes representing protobuf declarations, and their root directory, discover
    * the Cloudstate entities and their properities.
    *
    * @param protobufClassesDirectory the root folder of where classes reside
    * @param protobufClasses the classes to inspect
    * @return the entities found
    */
  @SuppressWarnings(
    Array(
      "org.wartremover.warts.AsInstanceOf",
      "org.wartremover.warts.Null",
      "org.wartremover.warts.Throw",
      "org.wartremover.warts.TryPartial"
    )
  )
  def introspectProtobufClasses(
      protobufClassesDirectory: Path,
      protobufClasses: Iterable[Path],
      failureReporter: Throwable => Unit
  ): Iterable[Entity] = {
    val exceptionHandler: PartialFunction[Throwable, Iterable[Entity]] = { case NonFatal(e) =>
      failureReporter(e)
      List.empty
    }

    try Using(
      URLClassLoader.newInstance(
        Array(protobufClassesDirectory.toUri().toURL()),
        classOf[ModelBuilder.type].getClassLoader()
      )
    ) { protobufClassLoader =>
      protobufClasses.flatMap { p =>
        val relativePath = protobufClassesDirectory.relativize(p)
        val packageName  = relativePath.getParent().toString().replace("/", ".")
        val className    = relativePath.toString().drop(packageName.size + 1).takeWhile(_ != '.')
        val fqn          = packageName + "." + className
        val method       = protobufClassLoader.loadClass(fqn).getMethod("getDescriptor")
        try {
          val descriptor = method.invoke(null).asInstanceOf[Descriptors.FileDescriptor]
          descriptor.getServices().asScala.map { service =>
            EventSourcedEntity(service.getFullName())
          }
        } catch exceptionHandler
      }
    }.get
    catch exceptionHandler
  }
}
