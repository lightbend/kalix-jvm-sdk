/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen

import scala.jdk.CollectionConverters._
import com.google.protobuf.Descriptors

import scala.util.matching.Regex

/**
  * Builds a model of entities and their properties from a protobuf descriptor
  */
object ModelBuilder {

  /**
    * An entity represents the primary model object and is conceptually equivalent to a class, or a type of state.
    */
  sealed abstract class Entity(
      val goPackage: Option[String],
      val javaOuterClassname: Option[String]
  )

  /**
    * A type of Entity that stores its state using a journal of events, and restores its state
    * by replaying that journal.
    */
  case class EventSourcedEntity(
      override val goPackage: Option[String],
      override val javaOuterClassname: Option[String],
      fullName: String,
      commands: Iterable[Command]
  ) extends Entity(goPackage, javaOuterClassname)

  /**
    * A command is used to express the intention to alter the state of an Entity.
    */
  case class Command(fullname: String, inputType: String, outputType: String)

  /**
    * Given a protobuf descriptor, discover the Cloudstate entities and their properties.
    *
    * Impure.
    *
    * @param descriptor the protobuf descriptor containing services entities
    * @param servicesPattern the pattern to use to identify service entities
    * @return the entities found
    */
  def introspectProtobufClasses(
      descriptor: Descriptors.FileDescriptor,
      servicesPattern: String
  ): Iterable[Entity] = {
    val servicesMatcher = new Regex(servicesPattern)

    val generalOptions = descriptor.getOptions.getAllFields.asScala
    val javaOuterClassName = generalOptions
      .find(_._1.getFullName == "google.protobuf.FileOptions.java_outer_classname")
      .map(_._2.toString)
    val goPackage = generalOptions
      .find(_._1.getFullName == "google.protobuf.FileOptions.go_package")
      .map(_._2.toString)

    descriptor.getServices.asScala
      .filter(service => servicesMatcher.matches(service.getFullName))
      .map { service =>
        val methods = service.getMethods.asScala
        val commands =
          methods.map(method =>
            Command(
              method.getFullName,
              method.getInputType.getFullName,
              method.getOutputType.getFullName
            )
          )
        EventSourcedEntity(
          goPackage,
          javaOuterClassName,
          service.getFullName,
          commands
        )
      }
  }
}
