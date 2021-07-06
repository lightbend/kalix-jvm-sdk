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

package com.akkaserverless.javasdk;

import akka.Done;
import akka.actor.ActorSystem;
import akka.annotation.ApiMayChange;
import akka.stream.Materializer;
import com.akkaserverless.javasdk.action.Action;
import com.akkaserverless.javasdk.action.ActionCreationContext;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity;
import com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntityOptions;
import com.akkaserverless.javasdk.impl.AnySupport;
import com.akkaserverless.javasdk.impl.action.ActionService;
import com.akkaserverless.javasdk.impl.action.AnnotationBasedActionSupport;
import com.akkaserverless.javasdk.impl.eventsourcedentity.AnnotationBasedEventSourcedSupport;
import com.akkaserverless.javasdk.impl.eventsourcedentity.EventSourcedEntityService;
import com.akkaserverless.javasdk.impl.replicatedentity.AnnotationBasedReplicatedEntitySupport;
import com.akkaserverless.javasdk.impl.replicatedentity.ReplicatedEntityStatefulService;
import com.akkaserverless.javasdk.impl.valueentity.AnnotationBasedEntitySupport;
import com.akkaserverless.javasdk.impl.valueentity.ValueEntityService;
import com.akkaserverless.javasdk.impl.view.AnnotationBasedViewSupport;
import com.akkaserverless.javasdk.impl.view.ViewService;
import com.akkaserverless.javasdk.lowlevel.ActionHandler;
import com.akkaserverless.javasdk.lowlevel.EventSourcedEntityFactory;
import com.akkaserverless.javasdk.lowlevel.ReplicatedEntityHandlerFactory;
import com.akkaserverless.javasdk.lowlevel.ValueEntityFactory;
import com.akkaserverless.javasdk.lowlevel.ViewFactory;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntity;
import com.akkaserverless.javasdk.replicatedentity.ReplicatedEntityOptions;
import com.akkaserverless.javasdk.valueentity.ValueEntity;
import com.akkaserverless.javasdk.valueentity.ValueEntityOptions;
import com.akkaserverless.javasdk.view.View;
import com.google.protobuf.Descriptors;
import com.typesafe.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

  public class LowLevelRegistration {
    /**
     * Register an event sourced entity factory.
     *
     * <p>This is a low level API intended for custom (eg, non reflection based) mechanisms for
     * implementing the entity.
     *
     * @param factory The event sourced factory.
     * @param descriptor The descriptor for the service that this entity implements.
     * @param entityType The persistence id for this entity.
     * @param snapshotEvery Specifies how snapshots of the entity state should be made: Zero means
     *     use default from configuration file. (Default) Any negative value means never snapshot.
     *     Any positive value means snapshot at-or-after that number of events.
     * @param entityOptions the options for this entity.
     * @param additionalDescriptors Any additional descriptors that should be used to look up
     *     protobuf types when needed.
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
              new EventSourcedEntityService(
                  factory,
                  descriptor,
                  newAnySupport(additionalDescriptors),
                  entityType,
                  snapshotEvery,
                  entityOptions));

      return AkkaServerless.this;
    }

    /**
     * Register a replicated entity factory.
     *
     * <p>This is a low level API intended for custom (eg, non reflection based) mechanisms for
     * implementing the entity.
     *
     * @param factory The replicated entity factory.
     * @param descriptor The descriptor for the service that this entity implements.
     * @param entityOptions The options for this entity.
     * @param additionalDescriptors Any additional descriptors that should be used to look up
     *     protobuf types when needed.
     * @return This stateful service builder.
     */
    public AkkaServerless registerReplicatedEntity(
        ReplicatedEntityHandlerFactory factory,
        Descriptors.ServiceDescriptor descriptor,
        ReplicatedEntityOptions entityOptions,
        Descriptors.FileDescriptor... additionalDescriptors) {

      services.put(
          descriptor.getFullName(),
          system ->
              new ReplicatedEntityStatefulService(
                  factory, descriptor, newAnySupport(additionalDescriptors), entityOptions));

      return AkkaServerless.this;
    }

    /**
     * Register an Action handler.
     *
     * <p>This is a low level API intended for custom (eg, non reflection based) mechanisms for
     * implementing the action.
     *
     * @param actionHandler The action handler.
     * @param descriptor The descriptor for the service that this action implements.
     * @param additionalDescriptors Any additional descriptors that should be used to look up
     *     protobuf types when needed.
     * @return This Akka Serverless builder.
     */
    public AkkaServerless registerAction(
        ActionHandler actionHandler,
        Descriptors.ServiceDescriptor descriptor,
        Descriptors.FileDescriptor... additionalDescriptors) {

      final AnySupport anySupport = newAnySupport(additionalDescriptors);

      ActionService service = new ActionService(context -> actionHandler, descriptor, anySupport);

      services.put(descriptor.getFullName(), system -> service);

      return AkkaServerless.this;
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
     * @return This stateful service builder.
     */
    public AkkaServerless registerValueEntity(
        ValueEntityFactory factory,
        Descriptors.ServiceDescriptor descriptor,
        String entityType,
        ValueEntityOptions entityOptions) {

      services.put(
          descriptor.getFullName(),
          system -> new ValueEntityService(factory, descriptor, entityType, entityOptions));

      return AkkaServerless.this;
    }

    /**
     * Register a view factory.
     *
     * <p>This is a low level API intended for custom (eg, non reflection based) mechanisms for
     * implementing the view.
     *
     * @param factory The view factory.
     * @param descriptor The descriptor for the service that this entity implements.
     * @param viewId The id of this view, used for persistence.
     * @param additionalDescriptors Any additional descriptors that should be used to look up
     *     protobuf types when needed.
     * @return This stateful service builder.
     */
    public AkkaServerless registerView(
        ViewFactory factory,
        Descriptors.ServiceDescriptor descriptor,
        String viewId,
        Descriptors.FileDescriptor... additionalDescriptors) {

      AnySupport anySupport = newAnySupport(additionalDescriptors);
      ViewService service =
          new ViewService(Optional.ofNullable(factory), descriptor, anySupport, viewId);
      services.put(descriptor.getFullName(), system -> service);

      return AkkaServerless.this;
    }
  }

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
   * com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity}.
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
   * com.akkaserverless.javasdk.eventsourcedentity.EventSourcedEntity}.
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
          entityClass
              + " does not declare an "
              + EventSourcedEntity.class.getName()
              + " annotation!");
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

    EventSourcedEntityService service =
        new EventSourcedEntityService(
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
   * Register an annotated replicated entity.
   *
   * <p>The entity class must be annotated with {@link ReplicatedEntity}.
   *
   * @param entityClass The entity class.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerReplicatedEntity(
      Class<?> entityClass,
      Descriptors.ServiceDescriptor descriptor,
      Descriptors.FileDescriptor... additionalDescriptors) {

    return registerReplicatedEntity(
        entityClass, descriptor, ReplicatedEntityOptions.defaults(), additionalDescriptors);
  }

  /**
   * Register an annotated replicated entity.
   *
   * <p>The entity class must be annotated with {@link ReplicatedEntity}.
   *
   * @param entityClass The entity class.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param entityOptions The options for this entity.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerReplicatedEntity(
      Class<?> entityClass,
      Descriptors.ServiceDescriptor descriptor,
      ReplicatedEntityOptions entityOptions,
      Descriptors.FileDescriptor... additionalDescriptors) {

    ReplicatedEntity entity = entityClass.getAnnotation(ReplicatedEntity.class);
    if (entity == null) {
      throw new IllegalArgumentException(
          entityClass
              + " does not declare an "
              + ReplicatedEntity.class.getName()
              + " annotation!");
    }

    final AnySupport anySupport = newAnySupport(additionalDescriptors);

    ReplicatedEntityStatefulService service =
        new ReplicatedEntityStatefulService(
            new AnnotationBasedReplicatedEntitySupport(entityClass, anySupport, descriptor),
            descriptor,
            anySupport,
            entityOptions);

    services.put(descriptor.getFullName(), system -> service);

    return this;
  }

  /**
   * Register an annotated Action service.
   *
   * <p>The action class must be annotated with {@link Action}.
   *
   * @param actionClass The action class, its constructor may accept an {@link
   *     ActionCreationContext}.
   * @param descriptor The descriptor for the service that this action implements.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This Akka Serverless builder.
   */
  public AkkaServerless registerAction(
      Class<?> actionClass,
      Descriptors.ServiceDescriptor descriptor,
      Descriptors.FileDescriptor... additionalDescriptors) {

    if (!actionClass.isAnnotationPresent(Action.class)) {
      throw new IllegalArgumentException(
          actionClass + " does not declare an " + Action.class.getName() + " annotation!");
    }
    final AnySupport anySupport = newAnySupport(additionalDescriptors);
    services.put(
        descriptor.getFullName(),
        system ->
            new ActionService(
                AnnotationBasedActionSupport.forClass(
                    actionClass, anySupport, descriptor, Materializer.matFromSystem(system)),
                descriptor,
                anySupport));
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
          entityClass + " does not declare an " + ValueEntity.class.getName() + " annotation!");
    }

    final String entityType;
    if (entity.entityType().isEmpty()) {
      entityType = entityClass.getSimpleName();
    } else {
      entityType = entity.entityType();
    }

    final AnySupport anySupport = newAnySupport(additionalDescriptors);
    ValueEntityService service =
        new ValueEntityService(
            new AnnotationBasedEntitySupport(entityClass, anySupport, descriptor),
            descriptor,
            entityType,
            entityOptions);

    services.put(descriptor.getFullName(), system -> service);

    return this;
  }

  /**
   * Experimental API: Register a view that has `transform_updates=false` set, so it can be handled
   * by the proxy.
   *
   * @param descriptor The descriptor of the view.
   * @param viewId The id of this view, used for persistence.
   * @param additionalDescriptors Any additional descriptors that may need to be loaded to support
   *     it.
   * @return This stateful service builder.
   */
  @ApiMayChange
  public AkkaServerless registerView(
      Descriptors.ServiceDescriptor descriptor,
      String viewId,
      Descriptors.FileDescriptor... additionalDescriptors) {

    AnySupport anySupport = newAnySupport(additionalDescriptors);
    ViewService service = new ViewService(Optional.empty(), descriptor, anySupport, viewId);
    services.put(descriptor.getFullName(), system -> service);

    return this;
  }

  /**
   * Register an annotated view.
   *
   * <p>The view class must be annotated with {@link com.akkaserverless.javasdk.view.View}.
   *
   * @param viewClass The view class.
   * @param descriptor The descriptor for the service that this entity implements.
   * @param viewId The id of this view, used for persistence.
   * @param additionalDescriptors Any additional descriptors that should be used to look up protobuf
   *     types when needed.
   * @return This stateful service builder.
   */
  public AkkaServerless registerView(
      Class<?> viewClass,
      Descriptors.ServiceDescriptor descriptor,
      String viewId,
      Descriptors.FileDescriptor... additionalDescriptors) {

    View view = viewClass.getAnnotation(View.class);
    if (view == null) {
      throw new IllegalArgumentException(
          viewClass + " does not declare an " + View.class.getName() + " annotation!");
    }

    AnySupport anySupport = newAnySupport(additionalDescriptors);
    ViewService service =
        new ViewService(
            Optional.of(new AnnotationBasedViewSupport(viewClass, anySupport, descriptor)),
            descriptor,
            anySupport,
            viewId);
    services.put(descriptor.getFullName(), system -> service);

    return this;
  }

  /**
   * This is a low level API intended for custom (eg, non reflection based) mechanisms for
   * implementing the components.
   */
  public AkkaServerless.LowLevelRegistration lowLevel() {
    return new LowLevelRegistration();
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
