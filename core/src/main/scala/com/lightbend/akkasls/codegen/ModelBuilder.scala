/*
 * Copyright (c) Lightbend Inc. 2021
 *
 */

package com.lightbend.akkasls.codegen

import scala.jdk.CollectionConverters._
import com.google.protobuf.Descriptors

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
      entityType: String,
      state: Option[String],
      commands: Iterable[Command],
      events: Iterable[String]
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
    * @param descriptors the protobuf descriptors containing service entities
    * @param servicesPattern the pattern to use to identify service entities
    * @return the entities found
    */
  def introspectProtobufClasses(
      descriptors: Iterable[Descriptors.FileDescriptor]
  ): Iterable[Entity] = {

    val entities =
      descriptors
        .flatMap(extractEventSourcedEntityDefinition)
        .map(entity => entity.fullName -> entity)
        .toMap

    descriptors
      .flatMap(_.getServices().asScala)
      .flatMap { service =>
        Option(
          service
            .getOptions()
            .getExtension(com.akkaserverless.Annotations.service)
            .getEntity()
            .getType()
        )
          .filter(_.nonEmpty)
          .map(resolveFullName(_, service.getFile().getPackage()))
          .flatMap(entities.get)
          .map { entity =>
            val generalOptions = service.getFile.getOptions.getAllFields.asScala
            val javaOuterClassName = generalOptions
              .find(_._1.getFullName == "google.protobuf.FileOptions.java_outer_classname")
              .map(_._2.toString)
            val goPackage = generalOptions
              .find(_._1.getFullName == "google.protobuf.FileOptions.go_package")
              .map(_._2.toString)

            val entityType = service.getName
            val methods    = service.getMethods.asScala
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
              entityType,
              entity.state,
              commands,
              entity.events
            )
          }

      }
  }

  /**
    * Resolves the provided name relative to the provided package
    *
    * @param name the name to resolve
    * @param pkg the package to resolve relative to
    * @return the resolved full name
    */
  private[codegen] def resolveFullName(name: String, pkg: String) = name.indexOf('.') match {
    case 0 => // name starts with a dot, treat as relative to package
      s"$pkg$name"
    case -1 => // name contains no dots, prepend package
      s"$pkg.$name"
    case _ => // name contains at least one dot, treat as absolute
      name
  }

  /**
    * Represents the parsed definition of an EventSourcedEntity, with all types resolved to their full names
    *
    * @param fullName the resolved full name of the entity
    * @param events the resolved full name of each event type the entity handles
    * @param state the resolved full name of the state type
    */
  private case class EventSourcedEntityDefinition(
      fullName: String,
      events: Iterable[String],
      state: Option[String]
  )

  /**
    * Extracts any defined event sourced entity from the provided protobuf file descriptor
    *
    * @param descriptor the file descriptor to extract from
    * @return the event sourced entity
    */
  private def extractEventSourcedEntityDefinition(
      descriptor: Descriptors.FileDescriptor
  ): Option[EventSourcedEntityDefinition] = {
    val generalOptions = descriptor.getOptions.getAllFields.asScala

    val rawEntity =
      descriptor
        .getOptions()
        .getExtension(com.akkaserverless.Annotations.file)
        .getEventSourcedEntity()

    Option(rawEntity.getName()).filter(_.nonEmpty).map { name =>
      val fullName = s"${descriptor.getPackage()}.${name}"
      EventSourcedEntityDefinition(
        fullName,
        rawEntity
          .getEventList()
          .asScala
          .map(event => resolveFullName(event.getType(), descriptor.getPackage())),
        Option(resolveFullName(rawEntity.getState().getType(), descriptor.getPackage()))
          .filter(_.nonEmpty)
      )
    }
  }
}
