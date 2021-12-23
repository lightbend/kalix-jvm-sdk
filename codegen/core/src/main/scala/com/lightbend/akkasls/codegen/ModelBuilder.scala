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

import com.akkaserverless.CodegenOptions
import com.akkaserverless.EventSourcedEntityDef
import com.akkaserverless.ReplicatedEntityDef
import com.akkaserverless.ValueEntityDef
import com.akkaserverless.ServiceOptions.ServiceType
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.ServiceDescriptor

/**
 * Builds a model of entities and their properties from a protobuf descriptor
 */
object ModelBuilder {

  /**
   * Convertor from Descriptor to FullyQualifiedName. Abstract because its implementation is different between Java and
   * Scala.
   *
   * (an alternative implementation could have been to pass the descriptor into FullyQualifiedName and put the logic on
   * the 'read side', but that makes testing with programmatically-generated names harder)
   */
  abstract class FullyQualifiedNameExtractor {
    def apply(descriptor: Descriptors.GenericDescriptor): FullyQualifiedName
    def fileDescriptorObject(descriptor: Descriptors.GenericDescriptor): FullyQualifiedName
    def packageName(descriptor: Descriptors.GenericDescriptor): PackageNaming
  }

  object Model {
    def empty: Model = Model(Map.empty, Map.empty)

    def fromService(service: Service): Model =
      Model.empty.addService(service)

    def fromEntity(entity: Entity): Model =
      Model.empty.addEntity(entity)
  }

  /**
   * The Akka Serverless service definitions and entities that could be extracted from a protobuf descriptor
   */
  case class Model(services: Map[String, Service], entities: Map[String, Entity]) {
    def lookupEntity(service: EntityService): Entity = {
      entities.getOrElse(
        service.componentFullName,
        throw new IllegalArgumentException(
          "Service [" + service.fqn.fullyQualifiedProtoName + "] refers to entity [" + service.componentFullName +
          s"], but no entity configuration is found for that component name. Entities: [${entities.keySet.mkString(", ")}]"))
    }

    def addService(service: Service): Model =
      copy(services + (service.fqn.fullyQualifiedProtoName -> service), entities)

    def addEntity(entity: Entity): Model =
      copy(services, entities + (entity.fqn.fullyQualifiedProtoName -> entity))

    def ++(model: Model): Model =
      Model(services ++ model.services, entities ++ model.entities)
  }

  /**
   * An entity represents the primary model object and is conceptually equivalent to a class, or a type of state.
   */
  sealed abstract class Entity(val fqn: FullyQualifiedName, val entityType: String) {
    val abstractEntityName = "Abstract" + fqn.name
    val routerName = fqn.name + "Router"
    val providerName = fqn.name + "Provider"
    val provider = fqn.deriveName(_ + "Provider")
    val impl = fqn.deriveName(identity)
  }

  /**
   * A type of Entity that stores its state using a journal of events, and restores its state by replaying that journal.
   */
  case class EventSourcedEntity(
      override val fqn: FullyQualifiedName,
      override val entityType: String,
      state: State,
      events: Iterable[Event])
      extends Entity(fqn, entityType)

  /**
   * A type of Entity that stores its current state directly.
   */
  case class ValueEntity(override val fqn: FullyQualifiedName, override val entityType: String, state: State)
      extends Entity(fqn, entityType)

  /**
   * A type of Entity that replicates its current state using CRDTs.
   */
  case class ReplicatedEntity(
      override val fqn: FullyQualifiedName,
      override val entityType: String,
      data: ReplicatedData)
      extends Entity(fqn, entityType)

  /**
   * The underlying replicated data type for a Replicated Entity.
   */
  sealed abstract class ReplicatedData(val shortName: String, val typeArguments: Iterable[TypeArgument]) {
    def this(shortName: String, typeArguments: TypeArgument*) = this(shortName, typeArguments)

    val name: String = "Replicated" + shortName
    val baseClass: String = name + "Entity"
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
  sealed trait TypeArgument

  object TypeArgument {

    def apply(fqn: FullyQualifiedName): TypeArgument =
      apply(fqn.name, fqn.parent, fqn.descriptorObject)

    def apply(name: String, proto: PackageNaming, fqn: Option[FullyQualifiedName]): TypeArgument = {
      if (name.nonEmpty && name.charAt(0).isLower) ScalarTypeArgument(ScalarType(name))
      else MessageTypeArgument(FullyQualifiedName(name, name, proto, fqn))
    }
  }

  /**
   * Type argument for Protobuf message types.
   */
  case class MessageTypeArgument(fqn: FullyQualifiedName) extends TypeArgument

  /**
   * Type argument for Protobuf scalar types.
   */
  case class ScalarTypeArgument(scalar: ScalarType) extends TypeArgument

  sealed trait ScalarType

  object ScalarType {
    case object Double extends ScalarType
    case object Float extends ScalarType
    case object Int32 extends ScalarType
    case object Int64 extends ScalarType
    case object UInt32 extends ScalarType
    case object UInt64 extends ScalarType
    case object SInt32 extends ScalarType
    case object SInt64 extends ScalarType
    case object Fixed32 extends ScalarType
    case object Fixed64 extends ScalarType
    case object SFixed32 extends ScalarType
    case object SFixed64 extends ScalarType
    case object Bool extends ScalarType
    case object String extends ScalarType
    case object Bytes extends ScalarType
    case object Unknown extends ScalarType

    def isScalarType(protoType: String): Boolean = apply(protoType) != Unknown

    def apply(protoType: String): ScalarType =
      protoType match {
        case "double"   => Double
        case "float"    => Float
        case "int32"    => Int32
        case "int64"    => Int64
        case "uint32"   => UInt32
        case "uint64"   => UInt64
        case "sint32"   => SInt32
        case "sint64"   => SInt64
        case "fixed32"  => Fixed32
        case "fixed64"  => Fixed64
        case "sfixed32" => SFixed32
        case "sfixed64" => SFixed64
        case "bool"     => Bool
        case "string"   => String
        case "bytes"    => Bytes
        case _          => Unknown
      }
  }

  /**
   * A Service backed by Akka Serverless; either an Action, View or Entity
   */
  sealed abstract class Service(val fqn: FullyQualifiedName, val commands: Iterable[Command]) {
    lazy val commandTypes =
      commands.flatMap { cmd =>
        cmd.inputType :: cmd.outputType :: Nil
      }
  }

  /**
   * A Service backed by an Action - a serverless function that is executed based on a trigger. The trigger could be an
   * HTTP or gRPC request or a stream of messages or events.
   */
  case class ActionService(
      override val fqn: FullyQualifiedName,
      override val commands: Iterable[Command],
      userDefinedNameOpt: Option[FullyQualifiedName])
      extends Service(fqn, commands) {

    val (baseClassName, className) =
      userDefinedNameOpt match {
        case Some(userDefinedName) => (userDefinedName.name, userDefinedName.name)
        case _ =>
          val baseCls = if (fqn.name.endsWith("Action")) fqn.name else fqn.name + "Action"
          val cls = if (fqn.name.contains("Action")) fqn.name + "Impl" else fqn.name + "Action"
          (baseCls, cls)
      }

    val impl = fqn.deriveName(_ => className)
    val abstractActionName = "Abstract" + baseClassName
    val routerName = baseClassName + "Router"
    val providerName = baseClassName + "Provider"
    val provider = fqn.deriveName(_ => baseClassName + "Provider")

    val classNameQualified = s"${fqn.parent.javaPackage}.$className"
    val providerNameQualified = s"${fqn.parent.javaPackage}.$providerName"
  }

  /**
   * A Service backed by a View, which provides a way to retrieve state from multiple Entities based on a query. You can
   * query non-key data items. You can create views from Value Entity state, Event Sourced Entity events, and by
   * subscribing to topics.
   */
  case class ViewService(
      override val fqn: FullyQualifiedName,
      /** all commands - queries and updates */
      override val commands: Iterable[Command],
      viewId: String,
      /** all updates, also non-transformed */
      updates: Iterable[Command],
      transformedUpdates: Iterable[Command],
      queries: Iterable[Command],
      userDefinedNameOpt: Option[FullyQualifiedName])
      extends Service(fqn, commands) {

    val (baseClassName, className) =
      userDefinedNameOpt match {
        case Some(userDefinedName) => (userDefinedName.name, userDefinedName.name)
        case _ =>
          val baseCls = if (fqn.name.endsWith("View")) fqn.name else fqn.name + "View"
          val cls = if (fqn.name.contains("View")) fqn.name + "Impl" else fqn.name + "View"
          (baseCls, cls)
      }

    val impl = fqn.deriveName(_ => className)
    val abstractViewName = "Abstract" + baseClassName
    val abstractView = fqn.deriveName(_ => abstractViewName)
    val routerName = baseClassName + "Router"
    val providerName = baseClassName + "Provider"
    val provider = fqn.deriveName(_ => baseClassName + "Provider")

    val classNameQualified = s"${fqn.parent.javaPackage}.$className"
    val providerNameQualified = s"${fqn.parent.javaPackage}.$providerName"

    if (updates.isEmpty)
      throw new IllegalArgumentException(
        s"At least one view method must have `option (akkaserverless.method).view.update` in ${fqn.protoName} (${fqn.parent.protoFileName}).")

    val state = State(updates.head.outputType)
  }

  /**
   * A Service backed by an Akka Serverless Entity
   */
  case class EntityService(
      override val fqn: FullyQualifiedName,
      override val commands: Iterable[Command],
      componentFullName: String)
      extends Service(fqn, commands)

  /**
   * A command is used to express the intention to alter the state of an Entity.
   */
  case class Command(
      name: String,
      inputType: FullyQualifiedName,
      outputType: FullyQualifiedName,
      streamedInput: Boolean,
      streamedOutput: Boolean,
      inFromTopic: Boolean,
      outToTopic: Boolean) {

    def isUnary: Boolean = !streamedInput && !streamedOutput
    def isStreamIn: Boolean = streamedInput && !streamedOutput
    def isStreamOut: Boolean = !streamedInput && streamedOutput
    def isStreamInOut: Boolean = streamedInput && streamedOutput
    def hasStream: Boolean = isStreamIn || isStreamOut || isStreamInOut

  }

  object Command {
    def from(method: Descriptors.MethodDescriptor)(implicit fqnExtractor: FullyQualifiedNameExtractor): Command = {
      val eventing = method.getOptions.getExtension(com.akkaserverless.Annotations.method).getEventing
      Command(
        method.getName,
        fqnExtractor(method.getInputType),
        fqnExtractor(method.getOutputType),
        streamedInput = method.isClientStreaming,
        streamedOutput = method.isServerStreaming,
        inFromTopic = eventing.hasIn && eventing.getIn.hasTopic,
        outToTopic = eventing.hasOut && eventing.getOut.hasTopic)
    }
  }

  /**
   * An event indicates that a change has occurred to an entity. Events are stored in a journal, and are read and
   * replayed each time the entity is reloaded by the Akka Serverless state management system.
   */
  case class Event(fqn: FullyQualifiedName)

  /**
   * The state is simply data — the current set of values for an entity instance. Event Sourced entities hold their
   * state in memory.
   */
  case class State(fqn: FullyQualifiedName)

  /**
   * Given a protobuf descriptor, discover the Akka Serverless entities and their properties.
   *
   * Impure.
   *
   * @param descriptors
   *   the protobuf descriptors containing service entities
   * @return
   *   the entities found
   */
  def introspectProtobufClasses(descriptors: Iterable[Descriptors.FileDescriptor])(implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): Model = {

    val descriptorSeq = descriptors.toSeq

    descriptorSeq.foldLeft(Model.empty) { case (accModel, fileDescriptor) =>
      log.debug("Looking at descriptor " + fileDescriptor.getName)

      val modelFromServices =
        fileDescriptor.getServices.asScala.foldLeft(accModel) { (model, serviceDescriptor) =>
          if (serviceDescriptor.getOptions.hasExtension(com.akkaserverless.Annotations.codegen)) {
            model ++ modelFromCodegenOptions(serviceDescriptor, descriptorSeq)

          } else if (serviceDescriptor.getOptions.hasExtension(com.akkaserverless.Annotations.service)) {
            // FIXME: old format, builds service model from old service annotation
            model ++ modelFromServiceOptions(serviceDescriptor)
          } else {
            model
          }
        }

      // FIXME: old format, builds entity model from domain.proto file
      val modelFromDomainFile =
        extractEventSourcedEntityDefinitionFromFileOptions(fileDescriptor, descriptorSeq) ++
        extractValueEntityDefinitionFromFileOptions(fileDescriptor, descriptorSeq) ++
        extractReplicatedEntityDefinitionFromFileOptions(fileDescriptor)

      modelFromServices ++ modelFromDomainFile
    }
  }

  /**
   * @return
   *   the FQN for a proto 'message' (which are used not just for "messages", but also for state types etc)
   */
  private def resolveFullyQualifiedMessageType(
      name: String,
      descriptor: Descriptors.FileDescriptor,
      descriptors: Seq[Descriptors.FileDescriptor])(implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): FullyQualifiedName = {
    // TODO this is used in the java tck as ValueEntity state type - I'm not sure we want to
    // support this? In that case we should probably support all primitives?
    if (name == "String")
      FullyQualifiedName.noDescriptor("String", PackageNaming("", "", "", javaMultipleFiles = true))
    else {
      val fullName = resolveFullName(descriptor.getPackage, name)
      val protoPackage = fullName.split("\\.").init.mkString(".")
      val protoName = fullName.split("\\.").last
      // TODO we could also look at the imports in the proto file to support
      // importing names from outside this file without using their fully qualified name.
      descriptors
        .filter(_.getPackage == protoPackage)
        .flatMap(_.getMessageTypes.asScala)
        .filter(_.getName == protoName) match {
        case Nil =>
          throw new IllegalStateException(
            s"No descriptor found for [$fullName] (searched: [${descriptors.map(_.getFile.getName).mkString(", ")}])")
        case Seq(descriptor) =>
          fqnExtractor.apply(descriptor)
        case matchingDescriptors =>
          throw new IllegalStateException(
            s"Multiple matching descriptors found for [$fullName] (searched: [${descriptors
              .map(_.getFile.getName)
              .mkString(", ")}], found in: ${matchingDescriptors.map(_.getFile.getName).mkString(", ")})")
      }
    }
  }

  /**
   * Lookup a FileDescriptor for the passed `package` and `name`.
   *
   * Valid inputs are:
   *   - package: foo.bar.baz, name: Foo
   *   - package: foo.bar, name: .baz.Foo
   *
   * The above input will trigger a lookup for a descriptor defining package "foo.bar.baz" and message "Foo"
   */
  private def lookupDescriptor(
      resolvedPackage: String,
      resolvedName: String,
      additionalDescriptors: Seq[Descriptors.FileDescriptor]): Descriptors.FileDescriptor = {

    // we should have only one match for package and resolvedName
    // if more than one proto defines the same message and package it will be caught earlier, by protoc
    additionalDescriptors
      .find { desc =>
        desc.getPackage == resolvedPackage && desc.getMessageTypes.asScala.exists(_.getName == resolvedName)
      }
      .getOrElse {
        throw new IllegalArgumentException(
          s"No descriptor found declaring package [$resolvedPackage] and message [$resolvedName]")
      }
  }

  /**
   * Resolves a proto 'message' using `package` and `name`.
   *
   * Valid inputs are:
   *   - package: foo.bar.baz, name: Foo
   *   - package: foo.bar, name: .baz.Foo
   *
   * The above input will trigger a lookup for a descriptor defining package "foo.bar.baz" and message "Foo"
   *
   * @return
   *   the FQN for a proto 'message' (which are used not just for "messages", but also for state types etc)
   */
  private def resolveFullyQualifiedMessageType(
      name: String,
      pkg: String,
      additionalDescriptors: Seq[Descriptors.FileDescriptor])(implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): FullyQualifiedName = {

    val (revolvedPackage, resolvedName) = extractPackageAndName(pkg, name)
    val descriptor = lookupDescriptor(revolvedPackage, resolvedName, additionalDescriptors)
    resolveFullyQualifiedMessageType(resolvedName, descriptor, additionalDescriptors)
  }
  private def resolveTypeArgument(name: String, pkg: String, additionalDescriptors: Seq[Descriptors.FileDescriptor])(
      implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): TypeArgument = {

    val (revolvedPackage, resolvedName) = extractPackageAndName(pkg, name)

    if (ScalarType.isScalarType(resolvedName))
      ScalarTypeArgument(ScalarType(resolvedName))
    else {
      val descriptor = lookupDescriptor(revolvedPackage, resolvedName, additionalDescriptors)
      MessageTypeArgument(resolveFullyQualifiedMessageType(resolvedName, descriptor, additionalDescriptors))
    }
  }

  /**
   * Resolves the provided name relative to the provided package
   *
   * @param name
   *   the name to resolve
   * @param pkg
   *   the package to resolve relative to
   * @return
   *   the resolved full name
   */
  private[codegen] def resolveFullName(pkg: String, name: String) =
    name.indexOf('.') match {
      case 0 => // name starts with a dot, treat as relative to package
        s"$pkg$name"
      case -1 => // name contains no dots, prepend package
        s"$pkg.$name"
      case _ => // name contains at least one dot, treat as absolute
        name
    }

  /**
   * Resolves the provided name relative to the provided package and split it back into name and package
   *
   * (foo.bar, baz.Foo) => (foo.bar.baz, Foo)
   */
  private def extractPackageAndName(pkg: String, name: String): (String, String) = {
    val resolvedPackageAndName = resolveFullName(pkg, name)
    val idx = resolvedPackageAndName.lastIndexOf('.')
    val resolvedName = resolvedPackageAndName.drop(idx + 1)
    val resolvedPackage = resolvedPackageAndName.take(idx)
    (resolvedPackage, resolvedName)
  }

  private def modelFromCodegenOptions(
      serviceDescriptor: ServiceDescriptor,
      additionalDescriptors: Seq[Descriptors.FileDescriptor])(implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): Model = {

    val codegenOptions = serviceDescriptor.getOptions.getExtension(com.akkaserverless.Annotations.codegen)
    val serviceName = fqnExtractor(serviceDescriptor)
    val methods = serviceDescriptor.getMethods.asScala
    val commands = methods.map(Command.from)

    codegenOptions.getCodegenCase match {
      case CodegenOptions.CodegenCase.ACTION =>
        val userDefinedName = buildUserDefinedName(codegenOptions.getAction.getName, serviceName)
        val actionService =
          ActionService(serviceName.asJavaMultiFiles, commands, userDefinedName)
        Model.fromService(actionService)

      case CodegenOptions.CodegenCase.VIEW =>
        val methodDetails = methods.flatMap { method =>
          Option(method.getOptions.getExtension(com.akkaserverless.Annotations.method).getView).map(viewOptions =>
            (method, viewOptions))
        }
        val updates = methodDetails.collect {
          case (method, viewOptions) if viewOptions.hasUpdate =>
            Command.from(method)
        }
        val transformedUpdates = methodDetails.collect {
          case (method, viewOptions) if viewOptions.hasUpdate && viewOptions.getUpdate.getTransformUpdates =>
            Command.from(method)
        }
        val queries = methodDetails.collect {
          case (method, viewOptions) if viewOptions.hasQuery =>
            Command.from(method)
        }

        val userDefinedName = buildUserDefinedName(codegenOptions.getView.getName, serviceName)
        val viewService =
          ViewService(
            serviceName.asJavaMultiFiles,
            commands,
            viewId = serviceDescriptor.getName,
            updates = updates,
            transformedUpdates = transformedUpdates,
            queries = queries,
            userDefinedName)
        Model.fromService(viewService)

      case CodegenOptions.CodegenCase.VALUE_ENTITY =>
        val entityDef = codegenOptions.getValueEntity
        val componentFullName = resolveFullComponentName(entityDef.getName, serviceName)
        Model
          .fromService(EntityService(serviceName, commands, componentFullName))
          .addEntity(extractValueEntity(serviceDescriptor, entityDef, additionalDescriptors))

      case CodegenOptions.CodegenCase.EVENT_SOURCED_ENTITY =>
        val entityDef = codegenOptions.getEventSourcedEntity
        val componentFullName = resolveFullComponentName(entityDef.getName, serviceName)
        Model
          .fromService(EntityService(serviceName, commands, componentFullName))
          .addEntity(extractEventSourcedEntity(serviceDescriptor, entityDef, additionalDescriptors))

      case CodegenOptions.CodegenCase.REPLICATED_ENTITY =>
        val entityDef = codegenOptions.getReplicatedEntity
        val componentFullName = resolveFullComponentName(entityDef.getName, serviceName)
        Model
          .fromService(EntityService(serviceName, commands, componentFullName))
          .addEntity(extractReplicatedEntity(serviceDescriptor, entityDef, additionalDescriptors))

      case _ => Model.empty
    }

  }

  /* if optionalName is empty (or null), full component name will be the same as the Service
   * otherwise, we need to resolve the entity name.
   */
  private def resolveFullComponentName(optionalName: String, serviceName: FullyQualifiedName) = {
    val fqn = defineEntityFullyQualifiedName(optionalName, serviceName)
    resolveFullName(fqn.parent.protoPackage, fqn.name)
  }

  private def nonEmptyName(name: String) =
    Option(name).filter(_.trim.nonEmpty)

  private def buildUserDefinedName(optionalName: String, serviceName: FullyQualifiedName) =
    nonEmptyName(optionalName)
      .map { name =>
        // if filled, we need to resolve the name
        // for example: pkg = foo.bar, name = baz.Qux
        // becomes: pkg = foo.bar.baz, name = Qux
        val (resolvedPackage, resolvedName) = extractPackageAndName(serviceName.parent.protoPackage, name)
        val descOpt = serviceName.descriptorObject.map { fqn =>
          fqn.copy(parent = fqn.parent.asJavaMultiFiles.changePackages(resolvedPackage))
        }
        val packageNaming =
          serviceName.parent.asJavaMultiFiles.changePackages(resolvedPackage)
        FullyQualifiedName(resolvedName, resolvedName, packageNaming, descOpt)
      }

  private def defineEntityFullyQualifiedName(optionalName: String, serviceName: FullyQualifiedName) =
    buildUserDefinedName(optionalName, serviceName)
      .getOrElse {
        // when an entity name is not explicitly defined, we need to fabricate a unique name
        // that doesn't conflict with the service name (since we do generate a grpc service for it)
        // therefore we append 'Entity' to the name
        serviceName
          .deriveName(_ + "Entity")
          .copy(protoName = serviceName.protoName + "Entity")
      }

  private def extractEventSourcedEntity(
      serviceProtoDescriptor: ServiceDescriptor,
      entityDef: EventSourcedEntityDef,
      additionalDescriptors: Seq[Descriptors.FileDescriptor])(implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): EventSourcedEntity = {

    val protoPackageName = serviceProtoDescriptor.getFile.getPackage

    EventSourcedEntity(
      defineEntityFullyQualifiedName(entityDef.getName, fqnExtractor(serviceProtoDescriptor)),
      entityDef.getEntityType,
      State(resolveFullyQualifiedMessageType(entityDef.getState, protoPackageName, additionalDescriptors)),
      entityDef.getEventsList.asScala.map { event =>
        Event(resolveFullyQualifiedMessageType(event, protoPackageName, additionalDescriptors))
      })

  }

  private def extractValueEntity(
      serviceProtoDescriptor: ServiceDescriptor,
      entityDef: ValueEntityDef,
      additionalDescriptors: Seq[Descriptors.FileDescriptor])(implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): ValueEntity = {

    val protoPackageName = serviceProtoDescriptor.getFile.getPackage
    ValueEntity(
      defineEntityFullyQualifiedName(entityDef.getName, fqnExtractor(serviceProtoDescriptor)),
      entityDef.getEntityType,
      State(resolveFullyQualifiedMessageType(entityDef.getState, protoPackageName, additionalDescriptors)))
  }

  private def extractReplicatedEntity(
      serviceProtoDescriptor: ServiceDescriptor,
      entityDef: ReplicatedEntityDef,
      additionalDescriptors: Seq[Descriptors.FileDescriptor])(implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): ReplicatedEntity = {

    import ReplicatedEntityDef.ReplicatedDataCase

    val protoPackageName = serviceProtoDescriptor.getFile.getPackage

    val dataType =
      entityDef.getReplicatedDataCase match {
        case ReplicatedDataCase.REPLICATED_COUNTER => ReplicatedCounter
        case ReplicatedDataCase.REPLICATED_REGISTER =>
          val typeArgument =
            resolveTypeArgument(entityDef.getReplicatedRegister.getValue, protoPackageName, additionalDescriptors)
          ReplicatedRegister(typeArgument)

        case ReplicatedDataCase.REPLICATED_SET =>
          val typeArgument =
            resolveTypeArgument(entityDef.getReplicatedSet.getElement, protoPackageName, additionalDescriptors)
          ReplicatedSet(typeArgument)

        case ReplicatedDataCase.REPLICATED_MAP =>
          val typeArgument =
            resolveTypeArgument(entityDef.getReplicatedMap.getKey, protoPackageName, additionalDescriptors)
          ReplicatedMap(typeArgument)

        case ReplicatedDataCase.REPLICATED_COUNTER_MAP =>
          val typeArgument =
            resolveTypeArgument(entityDef.getReplicatedCounterMap.getKey, protoPackageName, additionalDescriptors)
          ReplicatedCounterMap(typeArgument)

        case ReplicatedDataCase.REPLICATED_REGISTER_MAP =>
          val typeArgumentKey =
            resolveTypeArgument(entityDef.getReplicatedRegisterMap.getKey, protoPackageName, additionalDescriptors)
          val typeArgumentValue =
            resolveTypeArgument(entityDef.getReplicatedRegisterMap.getValue, protoPackageName, additionalDescriptors)
          ReplicatedRegisterMap(typeArgumentKey, typeArgumentValue)

        case ReplicatedDataCase.REPLICATED_MULTI_MAP =>
          val typeArgumentKey =
            resolveTypeArgument(entityDef.getReplicatedMultiMap.getKey, protoPackageName, additionalDescriptors)
          val typeArgumentValue =
            resolveTypeArgument(entityDef.getReplicatedMultiMap.getValue, protoPackageName, additionalDescriptors)
          ReplicatedMultiMap(typeArgumentKey, typeArgumentValue)

        case ReplicatedDataCase.REPLICATED_VOTE =>
          ReplicatedVote

        case ReplicatedDataCase.REPLICATEDDATA_NOT_SET =>
          throw new IllegalArgumentException("Replicated data type not set")
      }

    ReplicatedEntity(
      defineEntityFullyQualifiedName(entityDef.getName, fqnExtractor(serviceProtoDescriptor)),
      entityDef.getEntityType,
      dataType)
  }

  private def modelFromServiceOptions(serviceDescriptor: Descriptors.ServiceDescriptor)(implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): Model = {

    val serviceOptions = serviceDescriptor.getOptions.getExtension(com.akkaserverless.Annotations.service)
    val serviceType = serviceOptions.getType
    val serviceName = fqnExtractor(serviceDescriptor)

    val methods = serviceDescriptor.getMethods.asScala
    val commands = methods.map(Command.from)

    serviceType match {
      case ServiceType.SERVICE_TYPE_ENTITY =>
        if (serviceOptions.getComponent eq null) Model.empty
        else {
          val componentName = serviceOptions.getComponent
          val componentFullName = resolveFullName(serviceDescriptor.getFile.getPackage, componentName)
          Model.fromService(EntityService(serviceName, commands, componentFullName))
        }
      case ServiceType.SERVICE_TYPE_ACTION =>
        Model.fromService(ActionService(serviceName.asJavaMultiFiles, commands, None))

      case ServiceType.SERVICE_TYPE_VIEW =>
        val methodDetails = methods.flatMap { method =>
          Option(method.getOptions.getExtension(com.akkaserverless.Annotations.method).getView).map(viewOptions =>
            (method, viewOptions))
        }
        val updates = methodDetails.collect {
          case (method, viewOptions) if viewOptions.hasUpdate =>
            Command.from(method)
        }
        Model.fromService(
          ViewService(
            serviceName.asJavaMultiFiles,
            commands,
            viewId = serviceDescriptor.getName,
            updates = updates,
            transformedUpdates = methodDetails
              .collect {
                case (method, viewOptions) if viewOptions.hasUpdate && viewOptions.getUpdate.getTransformUpdates =>
                  Command.from(method)
              },
            queries = methodDetails.collect {
              case (method, viewOptions) if viewOptions.hasQuery =>
                Command.from(method)
            },
            None))

      case _ => Model.empty
    }
  }

  /**
   * Extracts any defined event sourced entity from the provided protobuf file descriptor
   *
   * @param descriptor
   *   the file descriptor to extract from
   * @return
   *   the event sourced entity
   */
  private def extractEventSourcedEntityDefinitionFromFileOptions(
      descriptor: Descriptors.FileDescriptor,
      additionalDescriptors: Seq[Descriptors.FileDescriptor])(implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): Model = {

    val entityDef =
      descriptor.getOptions
        .getExtension(com.akkaserverless.Annotations.file)
        .getEventSourcedEntity

    val protoReference = fqnExtractor.packageName(descriptor)
    val fullQualifiedDescriptor = Some(fqnExtractor.fileDescriptorObject(descriptor.getFile))

    nonEmptyName(entityDef.getName)
      .map { name =>
        Model.fromEntity(
          EventSourcedEntity(
            FullyQualifiedName(name, name, protoReference.asJavaMultiFiles, fullQualifiedDescriptor),
            entityDef.getEntityType,
            State(resolveFullyQualifiedMessageType(entityDef.getState, descriptor, additionalDescriptors)),
            entityDef.getEventsList.asScala
              // TODO this assumes events are defined in the same proto as the entity. To lift this restriction,
              // use something like resolveFullyQualifiedMessageType above
              .map(event => Event(FullyQualifiedName(event, event, protoReference, fullQualifiedDescriptor)))))
      }
      .getOrElse(Model.empty)
  }

  /**
   * Extracts any defined value entity from the provided protobuf file descriptor
   *
   * @param descriptor
   *   the file descriptor to extract from
   */
  private def extractValueEntityDefinitionFromFileOptions(
      descriptor: Descriptors.FileDescriptor,
      descriptors: Seq[Descriptors.FileDescriptor])(implicit
      log: Log,
      fqnExtractor: FullyQualifiedNameExtractor): Model = {
    val rawEntity =
      descriptor.getOptions
        .getExtension(com.akkaserverless.Annotations.file)
        .getValueEntity

    Option(rawEntity.getName)
      .filter(_.nonEmpty)
      .map { name =>
        Model.fromEntity(
          ValueEntity(
            FullyQualifiedName(
              name,
              name,
              fqnExtractor.packageName(descriptor).asJavaMultiFiles,
              Some(fqnExtractor.fileDescriptorObject(descriptor.getFile))),
            rawEntity.getEntityType,
            State(resolveFullyQualifiedMessageType(rawEntity.getState, descriptor, descriptors))))
      }
      .getOrElse(Model.empty)
  }

  /**
   * Extracts any defined replicated entity from the provided protobuf file descriptor
   *
   * @param descriptor
   *   the file descriptor to extract from
   */
  private def extractReplicatedEntityDefinitionFromFileOptions(
      descriptor: Descriptors.FileDescriptor)(implicit log: Log, fqnExtractor: FullyQualifiedNameExtractor): Model = {
    import com.akkaserverless.ReplicatedEntity.ReplicatedDataCase

    val rawEntity =
      descriptor.getOptions
        .getExtension(com.akkaserverless.Annotations.file)
        .getReplicatedEntity

    val protoReference = fqnExtractor.packageName(descriptor)
    val fullQualifiedDescriptor = Some(fqnExtractor.fileDescriptorObject(descriptor.getFile))

    Option(rawEntity.getName)
      .filter(_.nonEmpty)
      .flatMap { name =>
        val dataType = rawEntity.getReplicatedDataCase match {
          case ReplicatedDataCase.REPLICATED_COUNTER =>
            Some(ReplicatedCounter)
          case ReplicatedDataCase.REPLICATED_REGISTER =>
            val value = TypeArgument(rawEntity.getReplicatedRegister.getValue, protoReference, fullQualifiedDescriptor)
            Some(ReplicatedRegister(value))
          case ReplicatedDataCase.REPLICATED_SET =>
            val element = TypeArgument(rawEntity.getReplicatedSet.getElement, protoReference, fullQualifiedDescriptor)
            Some(ReplicatedSet(element))
          case ReplicatedDataCase.REPLICATED_MAP =>
            val key = TypeArgument(rawEntity.getReplicatedMap.getKey, protoReference, fullQualifiedDescriptor)
            Some(ReplicatedMap(key))
          case ReplicatedDataCase.REPLICATED_COUNTER_MAP =>
            val key = TypeArgument(rawEntity.getReplicatedCounterMap.getKey, protoReference, fullQualifiedDescriptor)
            Some(ReplicatedCounterMap(key))
          case ReplicatedDataCase.REPLICATED_REGISTER_MAP =>
            val key = TypeArgument(rawEntity.getReplicatedRegisterMap.getKey, protoReference, fullQualifiedDescriptor)
            val value =
              TypeArgument(rawEntity.getReplicatedRegisterMap.getValue, protoReference, fullQualifiedDescriptor)
            Some(ReplicatedRegisterMap(key, value))
          case ReplicatedDataCase.REPLICATED_MULTI_MAP =>
            val key = TypeArgument(rawEntity.getReplicatedMultiMap.getKey, protoReference, fullQualifiedDescriptor)
            val value = TypeArgument(rawEntity.getReplicatedMultiMap.getValue, protoReference, fullQualifiedDescriptor)
            Some(ReplicatedMultiMap(key, value))
          case ReplicatedDataCase.REPLICATED_VOTE =>
            Some(ReplicatedVote)
          case ReplicatedDataCase.REPLICATEDDATA_NOT_SET =>
            None
        }
        dataType
          .map { data =>
            Model.fromEntity(
              ReplicatedEntity(
                FullyQualifiedName(name, name, protoReference.asJavaMultiFiles, fullQualifiedDescriptor),
                rawEntity.getEntityType,
                data))
          }
      }
      .getOrElse(Model.empty)
  }
}
