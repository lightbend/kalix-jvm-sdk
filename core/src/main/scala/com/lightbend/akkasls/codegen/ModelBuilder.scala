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
      val serviceProto: ProtoReference
  )

  /**
    * A type of Entity that stores its state using a journal of events, and restores its state
    * by replaying that journal.
    */
  case class EventSourcedEntity(
      override val serviceProto: ProtoReference,
      fullName: String,
      entityType: String,
      state: Option[TypeReference],
      commands: Iterable[Command],
      events: Iterable[TypeReference]
  ) extends Entity(serviceProto)

  /**
    * A command is used to express the intention to alter the state of an Entity.
    */
  case class Command(fullname: String, inputType: TypeReference, outputType: TypeReference)

  /**
    * The details of the proto file that an entity or related message type have been derived from.
    */
  case class ProtoReference(
      fileName: String,
      pkg: String,
      goPackage: Option[String],
      javaPackageOption: Option[String],
      javaOuterClassname: Option[String]
  ) {
    lazy val javaPackage = javaPackageOption.getOrElse(pkg)
  }

  /**
    * The reference to a message type defined in protobuf
    * This contains enough detail to reconstruct references such as absolute Java packages
    */
  case class TypeReference(
      name: String,
      parent: ProtoReference
  ) {
    lazy val fullName = s"${parent.pkg}.$name"
  }

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

    println(entities)

    descriptors
      .flatMap(_.getServices().asScala)
      .flatMap { service =>
        println(service.getName())
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
            val serviceProto = ProtoReference.fromFileDescriptor(service.getFile())
            val entityType   = service.getName
            val methods      = service.getMethods.asScala
            val commands =
              methods.map(method =>
                Command(
                  method.getFullName,
                  TypeReference.fromDescriptor(method.getInputType),
                  TypeReference.fromDescriptor(method.getOutputType)
                )
              )

            EventSourcedEntity(
              serviceProto,
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
      events: Iterable[TypeReference],
      state: Option[TypeReference]
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

    val protoReference = ProtoReference.fromFileDescriptor(descriptor)

    Option(rawEntity.getName()).filter(_.nonEmpty).map { name =>
      val fullName = s"${descriptor.getPackage()}.${name}"
      EventSourcedEntityDefinition(
        fullName,
        rawEntity
          .getEventList()
          .asScala
          .map(event => TypeReference(event.getType(), protoReference)),
        Option(rawEntity.getState().getType())
          .filter(_.nonEmpty)
          .map(TypeReference(_, protoReference))
      )
    }
  }

  private object ProtoReference {
    def fromFileDescriptor(descriptor: Descriptors.FileDescriptor) = {
      val generalOptions = descriptor.getOptions.getAllFields.asScala
      val goPackage = generalOptions
        .find(_._1.getFullName == "google.protobuf.FileOptions.go_package")
        .map(_._2.toString)
      val javaPackage = generalOptions
        .find(_._1.getFullName == "google.protobuf.FileOptions.java_package")
        .map(_._2.toString)
      val javaOuterClassname = generalOptions
        .find(_._1.getFullName == "google.protobuf.FileOptions.java_outer_classname")
        .map(_._2.toString)

      ProtoReference(
        descriptor.getName(),
        descriptor.getPackage(),
        goPackage,
        javaPackage,
        javaOuterClassname
      )
    }
  }

  private object TypeReference {
    def fromDescriptor(descriptor: Descriptors.Descriptor) =
      TypeReference(descriptor.getName(), ProtoReference.fromFileDescriptor(descriptor.getFile()))
  }
}
