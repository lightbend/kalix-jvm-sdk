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
      val fqn: FullyQualifiedName,
      val entityType: String
  )

  /**
    * A type of Entity that stores its state using a journal of events, and restores its state
    * by replaying that journal.
    */
  case class EventSourcedEntity(
      override val fqn: FullyQualifiedName,
      override val entityType: String,
      state: Option[State],
      events: Iterable[Event]
  ) extends Entity(fqn, entityType)

  /**
    * A type of Entity that stores its state using a journal of events, and restores its state
    * by replaying that journal.
    */
  case class ValueEntity(
      override val fqn: FullyQualifiedName,
      override val entityType: String,
      state: Option[State]
  ) extends Entity(fqn, entityType)

  /**
    * A Service backed by an Akka Serverless entity
    */
  case class Service(
      fqn: FullyQualifiedName,
      entity: Entity,
      commands: Iterable[Command]
  )

  /**
    * A command is used to express the intention to alter the state of an Entity.
    */
  case class Command(
      fqn: FullyQualifiedName,
      inputType: FullyQualifiedName,
      outputType: FullyQualifiedName
  )

  /**
    * An event indicates that a change has occurred to an entity. Events are stored in a journal,
    * and are read and replayed each time the entity is reloaded by the Akka Serverless state
    * management system.
    */
  case class Event(fqn: FullyQualifiedName)

  /**
    * The state is simply data—​the current set of values for an entity instance.
    * Event Sourced entities hold their state in memory.
    */
  case class State(fqn: FullyQualifiedName)

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
  ): Iterable[Service] = {

    val entities = (
      descriptors.flatMap[Entity](extractEventSourcedEntityDefinition) ++
        descriptors.flatMap[Entity](extractValueEntityDefinition)
    ).map(entity => entity.fqn.fullName -> entity).toMap

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
            val serviceName = FullyQualifiedName.from(service)
            val entityType  = service.getName
            val methods     = service.getMethods.asScala
            val commands =
              methods.map(method =>
                Command(
                  FullyQualifiedName.from(method),
                  FullyQualifiedName.from(method.getInputType),
                  FullyQualifiedName.from(method.getOutputType)
                )
              )

            Service(
              serviceName,
              entity,
              commands
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
      events: Iterable[Event],
      state: Option[State]
  )

  /**
    * Extracts any defined event sourced entity from the provided protobuf file descriptor
    *
    * @param descriptor the file descriptor to extract from
    * @return the event sourced entity
    */
  private def extractEventSourcedEntityDefinition(
      descriptor: Descriptors.FileDescriptor
  ): Option[EventSourcedEntity] = {
    val generalOptions = descriptor.getOptions.getAllFields.asScala

    val rawEntity =
      descriptor
        .getOptions()
        .getExtension(com.akkaserverless.Annotations.file)
        .getEventSourcedEntity()

    val protoReference = PackageNaming.from(descriptor)

    Option(rawEntity.getName()).filter(_.nonEmpty).map { name =>
      val fullName = s"${descriptor.getPackage()}.${name}"
      EventSourcedEntity(
        FullyQualifiedName(name, protoReference),
        rawEntity.getEntityType(),
        Option(rawEntity.getState().getType())
          .filter(_.nonEmpty)
          .map(name => State(FullyQualifiedName(name, protoReference))),
        rawEntity
          .getEventList()
          .asScala
          .map(event => Event(FullyQualifiedName(event.getType(), protoReference)))
      )
    }
  }

  /**
    * Extracts any defined value entity from the provided protobuf file descriptor
    *
    * @param descriptor the file descriptor to extract from
    */
  private def extractValueEntityDefinition(
      descriptor: Descriptors.FileDescriptor
  ): Option[ValueEntity] = {
    val generalOptions = descriptor.getOptions.getAllFields.asScala

    val rawEntity =
      descriptor
        .getOptions()
        .getExtension(com.akkaserverless.Annotations.file)
        .getValueEntity()

    val protoReference = PackageNaming.from(descriptor)

    Option(rawEntity.getName()).filter(_.nonEmpty).map { name =>
      val fullName = s"${descriptor.getPackage()}.${name}"
      ValueEntity(
        FullyQualifiedName(name, protoReference),
        rawEntity.getEntityType(),
        Option(rawEntity.getState().getType())
          .filter(_.nonEmpty)
          .map(name => State(FullyQualifiedName(name, protoReference)))
      )
    }
  }
}
