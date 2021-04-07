/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.testkit.junit.jupiter;

import akka.actor.ClassicActorSystemProvider;
import akka.grpc.GrpcClientSettings;
import akka.grpc.javadsl.AkkaGrpcClient;
import com.akkaserverless.javasdk.AkkaServerless;
import com.akkaserverless.javasdk.testkit.AkkaServerlessTestkit;
import org.junit.jupiter.api.extension.*;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

class AkkaServerlessTestkitExtension implements BeforeAllCallback, ParameterResolver {

  private static final Namespace NAMESPACE = Namespace.create(AkkaServerlessTestkitExtension.class);
  private static final String TESTKIT = "testkit";

  @Override
  public void beforeAll(ExtensionContext context) {
    Class<?> testClass = context.getRequiredTestClass();
    AkkaServerless akkaServerless = findAkkaServerlessDescriptor(testClass);
    AkkaServerlessTestkit testkit = new AkkaServerlessTestkit(akkaServerless).start();
    context.getStore(NAMESPACE).put(TESTKIT, new StoredTestkit(testkit));
  }

  private static AkkaServerless findAkkaServerlessDescriptor(final Class<?> testClass) {
    return ReflectionUtils.findFields(
            testClass,
            AkkaServerlessTestkitExtension::isAkkaServerlessDescriptor,
            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
        .stream()
        .findFirst()
        .map(AkkaServerlessTestkitExtension::getAkkaServerlessDescriptor)
        .orElseThrow(
            () ->
                new ExtensionConfigurationException(
                    "No field annotated with @AkkaServerlessDescriptor found for @AkkaServerlessTest"));
  }

  private static boolean isAkkaServerlessDescriptor(final Field field) {
    if (AnnotationSupport.isAnnotated(field, AkkaServerlessDescriptor.class)) {
      if (AkkaServerless.class.isAssignableFrom(field.getType())) {
        return true;
      } else {
        throw new ExtensionConfigurationException(
            String.format(
                "Field [%s] annotated with @AkkaServerlessDescriptor is not an AkkaServerless",
                field.getName()));
      }
    } else {
      return false;
    }
  }

  private static AkkaServerless getAkkaServerlessDescriptor(final Field field) {
    return (AkkaServerless)
        ReflectionUtils.tryToReadFieldValue(field)
            .getOrThrow(
                e ->
                    new ExtensionConfigurationException(
                        "Cannot access AkkaServerless defined in field " + field.getName(), e));
  }

  @Override
  public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext context) {
    Class<?> type = parameterContext.getParameter().getType();
    return (type == AkkaServerlessTestkit.class) || AkkaGrpcClient.class.isAssignableFrom(type);
  }

  @Override
  public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context) {
    Class<?> type = parameterContext.getParameter().getType();
    Store store = context.getStore(NAMESPACE);
    AkkaServerlessTestkit testkit = store.get(TESTKIT, StoredTestkit.class).getTestkit();
    if (type == AkkaServerlessTestkit.class) {
      return testkit;
    } else if (AkkaGrpcClient.class.isAssignableFrom(type)) {
      return store
          .getOrComputeIfAbsent(
              type,
              key -> {
                Method createMethod =
                    ReflectionUtils.getRequiredMethod(
                        type, "create", GrpcClientSettings.class, ClassicActorSystemProvider.class);
                return new StoredGrpcClient(
                    (AkkaGrpcClient)
                        ReflectionUtils.invokeMethod(
                            createMethod,
                            null,
                            testkit.getGrpcClientSettings(),
                            testkit.getActorSystem()));
              },
              StoredGrpcClient.class)
          .getClient();
    } else {
      throw new ParameterResolutionException("Unexpected parameter type " + type);
    }
  }

  // Wrap testkit in CloseableResource, auto-closed when test finishes (extension store is closed)
  private static class StoredTestkit implements Store.CloseableResource {
    private final AkkaServerlessTestkit testkit;

    private StoredTestkit(AkkaServerlessTestkit testkit) {
      this.testkit = testkit;
    }

    public AkkaServerlessTestkit getTestkit() {
      return testkit;
    }

    @Override
    public void close() {
      testkit.stop();
    }
  }

  // Wrap clients in CloseableResource, auto-closed when test finishes (extension store is closed)
  private static class StoredGrpcClient implements Store.CloseableResource {
    private final AkkaGrpcClient client;

    private StoredGrpcClient(AkkaGrpcClient client) {
      this.client = client;
    }

    public AkkaGrpcClient getClient() {
      return client;
    }

    @Override
    public void close() {
      client.close();
    }
  }
}
