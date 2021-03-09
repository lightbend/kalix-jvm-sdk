/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk;

import akka.Done;
import akka.actor.ActorSystem;
import akka.stream.Materializer;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionHandler;
import com.akkaserverless.javasdk.crdt.CrdtEntity;
import com.akkaserverless.javasdk.crdt.CrdtEntityFactory;
import com.akkaserverless.javasdk.crdt.CrdtEntityOptions;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.javasdk.valueentity.ValueEntityFactory;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.akkaserverless.javasdk.eventsourced.EventSourcedEntity;
import com.akkaserverless.javasdk.eventsourced.EventSourcedEntityFactory;
import com.akkaserverless.javasdk.eventsourced.EventSourcedEntityOptions;
import com.akkaserverless.javasdk.impl.AnySupport;
import com.akkaserverless.javasdk.impl.action.ActionService;
import com.akkaserverless.javasdk.impl.action.AnnotationBasedActionSupport;
import com.akkaserverless.javasdk.impl.crdt.AnnotationBasedCrdtSupport;
import com.akkaserverless.javasdk.impl.crdt.CrdtStatefulService;
import com.akkaserverless.javasdk.impl.entity.AnnotationBasedEntitySupport;
import com.akkaserverless.javasdk.impl.entity.ValueEntityStatefulService;
import com.akkaserverless.javasdk.impl.eventsourced.AnnotationBasedEventSourcedSupport;
import com.akkaserverless.javasdk.impl.eventsourced.EventSourcedStatefulService;
import com.google.protobuf.Descriptors;
import com.typesafe.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

/**
 * The AkkaServerless class is the main interface to configuring entities to deploy, and
 * subsequently starting a local server which will expose these entities to the AkkaServerless Proxy
 * Sidecar.
 */
public final class AkkaServerless {
  private final Map<String, Function<ActorSystem, Service>> services = new HashMap<>();
  private ClassLoader classLoader = getClass().getClassLoader();
  private String typeUrlPrefix = AnySupport.DefaultTypeUrlPrefix();
  private AnySupport.Prefer prefer = AnySupport.PREFER_JAVA();

  /**
   * Sets the ClassLoader to be used for reflective access, the default value is the ClassLoader of
   * the AkkaServerless class.
   *
   * @param classLoader A non-null ClassLoader to be used for reflective access.
   * @return This AkkaServerless instance.
   */
  public AkkaServerless withClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
    return this;
  }

  /**
   * Sets the type URL prefix to be used when serializing and deserializing types from and to
   * Protobyf Any values. Defaults to "type.googleapis.com".
   *
   * @param prefix the type URL prefix to be used.
   * @return This AkkaServerless instance.
   */
  public AkkaServerless withTypeUrlPrefix(String prefix) {
    this.typeUrlPrefix = prefix;
    return this;
  }

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the
   * classpath, this specifies that Java should be preferred.
   *
   * @return This AkkaServerless instance.
   */
  public AkkaServerless preferJavaProtobufs() {
    this.prefer = AnySupport.PREFER_JAVA();
    return this;
  }

  /**
   * When locating protobufs, if both a Java and a ScalaPB generated class is found on the
   * classpath, this specifies that Scala should be preferred.
   *
   * @return This AkkaServerless instance.
   */
  public AkkaServerless preferScalaProtobufs() {
    this.prefer = AnySupport.PREFER_SCALA();
    return this;
  }

  /**
   * Register an annotated event sourced entity.
   *
   * <p>The entity class must be annotated with {@link
   * com.akkaserverless.javasdk.eventsourced.EventSourcedEntity}.
   *
   * @param entityClass The entity class.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerEventSourcedEntity(
      Class<?> entityClass,
      Descriptors.ServiceDescriptor descriptor,
      Descriptors.FileDescriptor... additionalDescriptors) {

    return registerEventSourcedEntity(
        entityClass, descriptor, EventSourcedEntityOptions.defaults(), additionalDescriptors);
  }

  /**
   * Register an annotated event sourced entity.
   *
   * <p>The entity class must be annotated with {@link
   * com.akkaserverless.javasdk.eventsourced.EventSourcedEntity}.
   *
   * @param entityClass The entity class.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param entityOptions The entity options.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerEventSourcedEntity(
      Class<?> entityClass,
      Descriptors.ServiceDescriptor descriptor,
      EventSourcedEntityOptions entityOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

    EventSourcedEntity entity = entityClass.getAnnotation(EventSourcedEntity.class);
    if (entity == null) {
      throw new IllegalArgumentException(
          entityClass + " does not declare an " + EventSourcedEntity.class + " annotation!");
    }
    if (descriptor == null) {
      throw new NullPointerException(
          "The ServiceDescriptor may not be null, verify the service lookup name.");
    }

    final String entityType;
    final int snapshotEvery;
    if (entity.entityType().isEmpty()) {
      entityType = entityClass.getSimpleName();
      snapshotEvery = 0; // Default
    } else {
      entityType = entity.entityType();
      snapshotEvery = entity.snapshotEvery();
    }

    final AnySupport anySupport = newAnySupport(additionalDescriptors);

    EventSourcedStatefulService service =
        new EventSourcedStatefulService(
            new AnnotationBasedEventSourcedSupport(entityClass, anySupport, descriptor),
            descriptor,
            anySupport,
            entityType,
            snapshotEvery,
            entityOptions);

    services.put(descriptor.getFullName(), system -> service);

    return this;
  }

  /**
   * Register an event sourced entity factory.
   *
   * <p>This is a low level API intended for custom (eg, non reflection based) mechanisms for
   * implementing the entity.
   *
   * @param factory The event sourced factory.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param entityType The persistence id for this entity.
   * @param snapshotEvery Specifies how snapshots of the entity state should be made: Zero means use
   *     default from configuration file. (Default) Any negative value means never snapshot. Any
   *     positive value means snapshot at-or-after that number of events.
   * @param entityOptions the options for this entity.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerEventSourcedEntity(
      EventSourcedEntityFactory factory,
      Descriptors.ServiceDescriptor descriptor,
      String entityType,
      int snapshotEvery,
      EventSourcedEntityOptions entityOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

    services.put(
        descriptor.getFullName(),
        system ->
            new EventSourcedStatefulService(
                factory,
                descriptor,
                newAnySupport(additionalDescriptors),
                entityType,
                snapshotEvery,
                entityOptions));

    return this;
  }

  /**
   * Register an annotated CRDT entity.
   *
   * <p>The entity class must be annotated with {@link com.akkaserverless.javasdk.crdt.CrdtEntity}.
   *
   * @param entityClass The entity class.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerCrdtEntity(
      Class<?> entityClass,
      Descriptors.ServiceDescriptor descriptor,
      Descriptors.FileDescriptor... additionalDescriptors) {

    return registerCrdtEntity(
        entityClass, descriptor, CrdtEntityOptions.defaults(), additionalDescriptors);
  }

  /**
   * Register an annotated CRDT entity.
   *
   * <p>The entity class must be annotated with {@link com.akkaserverless.javasdk.crdt.CrdtEntity}.
   *
   * @param entityClass The entity class.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param entityOptions The options for this entity.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerCrdtEntity(
      Class<?> entityClass,
      Descriptors.ServiceDescriptor descriptor,
      CrdtEntityOptions entityOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

    CrdtEntity entity = entityClass.getAnnotation(CrdtEntity.class);
    if (entity == null) {
      throw new IllegalArgumentException(
          entityClass + " does not declare an " + CrdtEntity.class + " annotation!");
    }

    final AnySupport anySupport = newAnySupport(additionalDescriptors);

    CrdtStatefulService service =
        new CrdtStatefulService(
            new AnnotationBasedCrdtSupport(entityClass, anySupport, descriptor),
            descriptor,
            anySupport,
            entityOptions);

    services.put(descriptor.getFullName(), system -> service);

    return this;
  }

  /**
   * Register a CRDT entity factory.
   *
   * <p>This is a low level API intended for custom (eg, non reflection based) mechanisms for
   * implementing the entity.
   *
   * @param factory The CRDT factory.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param entityOptions The options for this entity.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerCrdtEntity(
      CrdtEntityFactory factory,
      Descriptors.ServiceDescriptor descriptor,
      CrdtEntityOptions entityOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

    services.put(
        descriptor.getFullName(),
        system ->
            new CrdtStatefulService(
                factory, descriptor, newAnySupport(additionalDescriptors), entityOptions));

    return this;
  }

  /**
   * Register an annotated Action service.
   *
   * <p>The action class must be annotated with {@link Action}.
   *
   * @param action The action object.
   * @param descriptor The descriptor for the service that this action implements.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This Akka Serverless builder.
   */
  public AkkaServerless registerAction(
      Object action,
      Descriptors.ServiceDescriptor descriptor,
      Descriptors.FileDescriptor... additionalDescriptors) {

    Action actionAnnotation = action.getClass().getAnnotation(Action.class);
    if (actionAnnotation == null) {
      throw new IllegalArgumentException(
          action.getClass() + " does not declare an " + Action.class + " annotation!");
    }

    final AnySupport anySupport = newAnySupport(additionalDescriptors);

    services.put(
        descriptor.getFullName(),
        system ->
            new ActionService(
                new AnnotationBasedActionSupport(
                    action, anySupport, descriptor, Materializer.createMaterializer(system)),
                descriptor,
                anySupport));

    return this;
  }

  /**
   * Register an Action handler.
   *
   * <p>This is a low level API intended for custom (eg, non reflection based) mechanisms for
   * implementing the action.
   *
   * @param actionHandler The action handler.
   * @param descriptor The descriptor for the service that this action implements.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This Akka Serverless builder.
   */
  public AkkaServerless registerAction(
      ActionHandler actionHandler,
      Descriptors.ServiceDescriptor descriptor,
      Descriptors.FileDescriptor... additionalDescriptors) {

    final AnySupport anySupport = newAnySupport(additionalDescriptors);

    ActionService service = new ActionService(actionHandler, descriptor, anySupport);

    services.put(descriptor.getFullName(), system -> service);

    return this;
  }

  /**
   * Register an annotated value based entity.
   *
   * <p>The entity class must be annotated with {@link ValueEntity}.
   *
   * @param entityClass The entity class.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerValueEntity(
      Class<?> entityClass,
      Descriptors.ServiceDescriptor descriptor,
      Descriptors.FileDescriptor... additionalDescriptors) {

    return registerValueEntity(
        entityClass, descriptor, ValueEntityOptions.defaults(), additionalDescriptors);
  }

  /**
   * Register an annotated value based entity.
   *
   * <p>The entity class must be annotated with {@link ValueEntity}.
   *
   * @param entityClass The entity class.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param entityOptions The options for this entity.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerValueEntity(
      Class<?> entityClass,
      Descriptors.ServiceDescriptor descriptor,
      ValueEntityOptions entityOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

    ValueEntity entity = entityClass.getAnnotation(ValueEntity.class);
    if (entity == null) {
      throw new IllegalArgumentException(
          entityClass + " does not declare an " + ValueEntity.class + " annotation!");
    }

    final String entityType;
    if (entity.entityType().isEmpty()) {
      entityType = entityClass.getSimpleName();
    } else {
      entityType = entity.entityType();
    }

    final AnySupport anySupport = newAnySupport(additionalDescriptors);
    ValueEntityStatefulService service =
        new ValueEntityStatefulService(
            new AnnotationBasedEntitySupport(entityClass, anySupport, descriptor),
            descriptor,
            anySupport,
            entityType,
            entityOptions);

    services.put(descriptor.getFullName(), system -> service);

    return this;
  }

  /**
   * Register a value based entity factory.
   *
   * <p>This is a low level API intended for custom (eg, non reflection based) mechanisms for
   * implementing the entity.
   *
   * @param factory The value based entity factory.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param entityType The entity type name
   * @param entityOptions The options for this entity.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerValueEntity(
      ValueEntityFactory factory,
      Descriptors.ServiceDescriptor descriptor,
      String entityType,
      ValueEntityOptions entityOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

    services.put(
        descriptor.getFullName(),
        system ->
            new ValueEntityStatefulService(
                factory,
                descriptor,
                newAnySupport(additionalDescriptors),
                entityType,
                entityOptions));

    return this;
  }

  /**
   * Starts a server with the configured entities.
   *
   * @return a CompletionStage which will be completed when the server has shut down.
   */
  public CompletionStage<Done> start() {
    return createRunner().run();
  }

  /**
   * Starts a server with the configured entities, using the supplied configuration.
   *
   * @return a CompletionStage which will be completed when the server has shut down.
   */
  public CompletionStage<Done> start(Config config) {
    return createRunner(config).run();
  }

  /**
   * Creates an AkkaServerlessRunner using the currently configured services. In order to start the
   * server, `run()` must be invoked on the returned AkkaServerlessRunner.
   *
   * @return an AkkaServerlessRunner
   */
  public AkkaServerlessRunner createRunner() {
    return new AkkaServerlessRunner(services);
  }

  /**
   * Creates an AkkaServerlessRunner using the currently configured services, using the supplied
   * configuration. In order to start the server, `run()` must be invoked on the returned
   * AkkaServerlessRunner.
   *
   * @return an AkkaServerlessRunner
   */
  public AkkaServerlessRunner createRunner(Config config) {
    return new AkkaServerlessRunner(services, config);
  }

  private AnySupport newAnySupport(Descriptors.FileDescriptor[] descriptors) {
    return new AnySupport(descriptors, classLoader, typeUrlPrefix, prefer);
  }
}
