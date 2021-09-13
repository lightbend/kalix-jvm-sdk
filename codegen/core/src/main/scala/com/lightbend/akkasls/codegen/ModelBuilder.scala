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
   * A type of Entity that stores its current state directly.
   */
  case class ValueEntity(
      override val fqn: FullyQualifiedName,
      override val entityType: String,
      state: State
  ) extends Entity(fqn, entityType)

  /**
   * A type of Entity that replicates its current state using CRDTs.
   */
  case class ReplicatedEntity(
      override val fqn: FullyQualifiedName,
      override val entityType: String,
      data: ReplicatedData
  ) extends Entity(fqn, entityType)

  /**
   * The underlying replicated data type for a Replicated Entity.
   */
  sealed abstract class ReplicatedData(
      val shortName: String,
      val typeArguments: Iterable[TypeArgument]
  ) {
    def this(shortName: String, typeArguments: TypeArgument*) = this(shortName, typeArguments)

    val name: String = "Replicated" + shortName
  }

  case object ReplicatedCounter extends ReplicatedData("Counter")

  case class ReplicatedRegister(value: TypeArgument) extends ReplicatedData("Register", value)

  case class ReplicatedSet(element: TypeArgument) extends ReplicatedData("Set", element)

  case class ReplicatedMap(key: TypeArgument) extends ReplicatedData("Map", key)

  case class ReplicatedCounterMap(key: TypeArgument) extends ReplicatedData("CounterMap", key)

  case class ReplicatedRegisterMap(key: TypeArgument, value: TypeArgument)
      extends ReplicatedData("RegisterMap", key, value)

  case class ReplicatedMultiMap(key: TypeArgument, value: TypeArgument) extends ReplicatedData("MultiMap", key, value)

  case object ReplicatedVote extends ReplicatedData("Vote")

  /**
   * Type argument for generic replicated data types with type parameters.
   */
  case class TypeArgument(fqn: FullyQualifiedName)

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
      /** all commands - queries and updates */
      override val commands: Iterable[Command],
      viewId: String,
      /** all updates, also non-transformed */
      updates: Iterable[Command],
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

    val state = State(updates.head.outputType)
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
      streamedOutput: Boolean,
      inFromTopic: Boolean,
      outToTopic: Boolean
  )

  object Command {
    def from(method: Descriptors.MethodDescriptor): Command = {
      val eventing = method.getOptions.getExtension(com.akkaserverless.Annotations.method).getEventing
      Command(
        FullyQualifiedName.from(method),
        FullyQualifiedName.from(method.getInputType),
        FullyQualifiedName.from(method.getOutputType),
        streamedInput = method.isClientStreaming,
        streamedOutput = method.isServerStreaming,
        inFromTopic = eventing.hasIn && eventing.getIn.hasTopic,
        outToTopic = eventing.hasOut && eventing.getOut.hasTopic
      )
    }
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
   * Given a protobuf descriptor, discover the Akka Serverless entities and their properties.
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
              val updates = methodDetails.collect {
                case (method, viewOptions) if viewOptions.hasUpdate =>
                  Command.from(method)
              }
              Some(
                ViewService(
                  serviceName,
                  commands,
                  viewId = serviceDescriptor.getName(),
                  updates = updates,
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
          extractValueEntityDefinition(descriptor).map(entity => entity.fqn.fullQualifiedName -> entity) ++
          extractReplicatedEntityDefinition(descriptor).map(entity => entity.fqn.fullQualifiedName -> entity)
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

  /**
   * Extracts any defined replicated entity from the provided protobuf file descriptor
   *
   * @param descriptor the file descriptor to extract from
   */
  private def extractReplicatedEntityDefinition(
      descriptor: Descriptors.FileDescriptor
  )(implicit log: Log): Option[ReplicatedEntity] = {
    import com.akkaserverless.ReplicatedEntity.ReplicatedDataCase

    val rawEntity =
      descriptor.getOptions
        .getExtension(com.akkaserverless.Annotations.file)
        .getReplicatedEntity
    log.debug("Raw replicated entity name: " + rawEntity.getName)

    val protoReference = PackageNaming.from(descriptor)

    Option(rawEntity.getName).filter(_.nonEmpty).flatMap { name =>
      val dataType = rawEntity.getReplicatedDataCase match {
        case ReplicatedDataCase.REPLICATED_COUNTER =>
          Some(ReplicatedCounter)
        case ReplicatedDataCase.REPLICATED_REGISTER =>
          val value = TypeArgument(FullyQualifiedName(rawEntity.getReplicatedRegister.getValue, protoReference))
          Some(ReplicatedRegister(value))
        case ReplicatedDataCase.REPLICATED_SET =>
          val element = TypeArgument(FullyQualifiedName(rawEntity.getReplicatedSet.getElement, protoReference))
          Some(ReplicatedSet(element))
        case ReplicatedDataCase.REPLICATED_MAP =>
          val key = TypeArgument(FullyQualifiedName(rawEntity.getReplicatedMap.getKey, protoReference))
          Some(ReplicatedMap(key))
        case ReplicatedDataCase.REPLICATED_COUNTER_MAP =>
          val key = TypeArgument(FullyQualifiedName(rawEntity.getReplicatedCounterMap.getKey, protoReference))
          Some(ReplicatedCounterMap(key))
        case ReplicatedDataCase.REPLICATED_REGISTER_MAP =>
          val key = TypeArgument(FullyQualifiedName(rawEntity.getReplicatedRegisterMap.getKey, protoReference))
          val value = TypeArgument(FullyQualifiedName(rawEntity.getReplicatedRegisterMap.getValue, protoReference))
          Some(ReplicatedRegisterMap(key, value))
        case ReplicatedDataCase.REPLICATED_MULTI_MAP =>
          val key = TypeArgument(FullyQualifiedName(rawEntity.getReplicatedMultiMap.getKey, protoReference))
          val value = TypeArgument(FullyQualifiedName(rawEntity.getReplicatedMultiMap.getValue, protoReference))
          Some(ReplicatedMultiMap(key, value))
        case ReplicatedDataCase.REPLICATED_VOTE =>
          Some(ReplicatedVote)
        case ReplicatedDataCase.REPLICATEDDATA_NOT_SET =>
          None
      }

      dataType.map { data =>
        ReplicatedEntity(
          FullyQualifiedName(name, protoReference),
          rawEntity.getEntityType,
          data
        )
      }
    }
  }

}
