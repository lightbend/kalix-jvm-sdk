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

import scala.jdk.CollectionConverters._
import com.google.protobuf.Descriptors
import com.akkaserverless.ServiceOptions.ServiceType

/**
 * Builds a model of entities and their properties from a protobuf descriptor
 */
object ModelBuilder {

  /**
   * The Akka Serverless service definitions and entities that could be extracted from a protobuf descriptor
   */
  case class Model(
      services: Map[String, Service],
      entities: Map[String, Entity]
  )

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
      state: State,
      events: Iterable[Event]
  ) extends Entity(fqn, entityType)

  /**
   * A type of Entity that stores its state using a journal of events, and restores its state
   * by replaying that journal.
   */
  case class ValueEntity(
      override val fqn: FullyQualifiedName,
      override val entityType: String,
      state: State
  ) extends Entity(fqn, entityType)

  /**
   * A Service backed by Akka Serverless; either an Action, View or Entity
   */
  sealed abstract class Service(
      val fqn: FullyQualifiedName,
      val commands: Iterable[Command]
  ) {
    lazy val commandTypes =
      commands.flatMap { cmd =>
        cmd.inputType :: cmd.outputType :: Nil
      }
  }

  /**
   * A Service backed by an Action - a serverless function that is executed based on a trigger.
   * The trigger could be an HTTP or gRPC request or a stream of messages or events.
   */
  case class ActionService(
      override val fqn: FullyQualifiedName,
      override val commands: Iterable[Command]
  ) extends Service(fqn, commands) {

    private val baseClassName =
      if (fqn.name.endsWith("Action")) fqn.name
      else fqn.name + "Action"

    val className =
      if (fqn.name.endsWith("Action")) fqn.name + "Impl"
      else fqn.name + "Action"
    val interfaceName = "Abstract" + baseClassName
    val handlerName = baseClassName + "Handler"
    val providerName = baseClassName + "Provider"

    val classNameQualified = s"${fqn.parent.javaPackage}.$className"
    val providerNameQualified = s"${fqn.parent.javaPackage}.$providerName"
  }

  /**
   * A Service backed by a View, which provides a way to retrieve state from multiple Entities based on a query.
   * You can query non-key data items. You can create views from Value Entity state, Event Sourced Entity events, and by subscribing to topics.
   */
  case class ViewService(
      override val fqn: FullyQualifiedName,
      override val commands: Iterable[Command],
      viewId: String,
      transformedUpdates: Iterable[Command]
  ) extends Service(fqn, commands) {

    val viewClassName =
      if (fqn.name.endsWith("View")) fqn.name
      else fqn.name + "View"

    val abstractViewName = "Abstract" + viewClassName
    val handlerName = viewClassName + "Handler"
    val providerName = viewClassName + "Provider"

    val classNameQualified = s"${fqn.parent.javaPackage}.$viewClassName"
    val providerNameQualified = s"${fqn.parent.javaPackage}.$providerName"

    val state = State(commands.head.outputType)
  }

  /**
   * A Service backed by an Akka Serverless Entity
   */
  case class EntityService(
      override val fqn: FullyQualifiedName,
      override val commands: Iterable[Command],
      componentFullName: String
  ) extends Service(fqn, commands)

  /**
   * A command is used to express the intention to alter the state of an Entity.
   */
  case class Command(
      fqn: FullyQualifiedName,
      inputType: FullyQualifiedName,
      outputType: FullyQualifiedName,
      streamedInput: Boolean,
      streamedOutput: Boolean
  )

  object Command {
    def from(method: Descriptors.MethodDescriptor): Command = Command(
      FullyQualifiedName.from(method),
      FullyQualifiedName.from(method.getInputType),
      FullyQualifiedName.from(method.getOutputType),
      streamedInput = method.isClientStreaming,
      streamedOutput = method.isServerStreaming
    )
  }

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
   * @return the entities found
   */
  def introspectProtobufClasses(
      descriptors: Iterable[Descriptors.FileDescriptor]
  )(implicit log: Log): Model =
    descriptors.foldLeft(Model(Map.empty, Map.empty)) {
      case (Model(existingServices, existingEntities), descriptor) =>
        log.debug("Looking at descriptor " + descriptor.getName)
        val services = for {
          serviceDescriptor <- descriptor.getServices.asScala
          options = serviceDescriptor
            .getOptions()
            .getExtension(com.akkaserverless.Annotations.service)
          serviceType <- Option(options.getType())
          serviceName = FullyQualifiedName.from(serviceDescriptor, serviceType)

          methods = serviceDescriptor.getMethods.asScala
          commands = methods.map(Command.from)

          service <- serviceType match {
            case ServiceType.SERVICE_TYPE_ENTITY =>
              Option(options.getComponent())
                .filter(_.nonEmpty)
                .map[Service] { componentName =>
                  val componentFullName =
                    resolveFullName(componentName, serviceDescriptor.getFile.getPackage)

                  EntityService(
                    serviceName,
                    commands,
                    componentFullName
                  )
                }
            case ServiceType.SERVICE_TYPE_ACTION =>
              Some(
                ActionService(
                  serviceName,
                  commands
                )
              )
            case ServiceType.SERVICE_TYPE_VIEW =>
              val methodDetails = methods.flatMap { method =>
                Option(
                  method.getOptions().getExtension(com.akkaserverless.Annotations.method).getView()
                ).map(viewOptions => (method, viewOptions))
              }
              Some(
                ViewService(
                  serviceName,
                  commands,
                  viewId = serviceDescriptor.getName(),
                  transformedUpdates = methodDetails
                    .collect {
                      case (method, viewOptions)
                          if viewOptions.hasUpdate && viewOptions
                            .getUpdate()
                            .getTransformUpdates() =>
                        Command.from(method)
                    }
                )
              )
            case _ => None
          }
        } yield serviceName.fullQualifiedName -> service

        Model(
          existingServices ++ services,
          existingEntities ++
          extractEventSourcedEntityDefinition(descriptor).map(entity => entity.fqn.fullQualifiedName -> entity) ++
          extractValueEntityDefinition(descriptor).map(entity => entity.fqn.fullQualifiedName -> entity)
        )
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
   * Extracts any defined event sourced entity from the provided protobuf file descriptor
   *
   * @param descriptor the file descriptor to extract from
   * @return the event sourced entity
   */
  private def extractEventSourcedEntityDefinition(
      descriptor: Descriptors.FileDescriptor
  ): Option[EventSourcedEntity] = {
    val rawEntity =
      descriptor.getOptions
        .getExtension(com.akkaserverless.Annotations.file)
        .getEventSourcedEntity

    val protoReference = PackageNaming.from(descriptor)

    Option(rawEntity.getName).filter(_.nonEmpty).map { name =>
      EventSourcedEntity(
        FullyQualifiedName(name, protoReference),
        rawEntity.getEntityType,
        State(FullyQualifiedName(rawEntity.getState, protoReference)),
        rawEntity.getEventsList.asScala
          .map(event => Event(FullyQualifiedName(event, protoReference)))
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
  )(implicit log: Log): Option[ValueEntity] = {
    val rawEntity =
      descriptor.getOptions
        .getExtension(com.akkaserverless.Annotations.file)
        .getValueEntity
    log.debug("Raw value entity name: " + rawEntity.getName)

    val protoReference = PackageNaming.from(descriptor)

    Option(rawEntity.getName).filter(_.nonEmpty).map { name =>
      ValueEntity(
        FullyQualifiedName(name, protoReference),
        rawEntity.getEntityType,
        State(FullyQualifiedName(rawEntity.getState, protoReference))
      )
    }
  }

}
