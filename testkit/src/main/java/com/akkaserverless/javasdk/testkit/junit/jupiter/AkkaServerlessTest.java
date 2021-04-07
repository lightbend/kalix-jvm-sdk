/*
 * Copyright 2019 Lightbend Inc.
 */

package com.akkaserverless.javasdk.testkit.junit.jupiter;

import com.akkaserverless.javasdk.testkit.AkkaServerlessTestkit;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @AkkaServerlessTest} registers a JUnit Jupiter extension to automatically manage the
 * lifecycle of {@link AkkaServerlessTestkit} and Akka gRPC clients.
 *
 * <p><b>Note</b>: JUnit Jupiter is not provided as a transitive dependency of the Java SDK testkit
 * module but must be added explicitly to your project.
 *
 * <p>The Akka Serverless Testkit extension finds a field annotated with {@link
 * AkkaServerlessDescriptor} and creates a testkit for this annotated {@code AkkaServerless}.
 *
 * <p>The testkit can be injected into constructors or test methods, by specifying an {@code
 * AkkaServerlessTestkit} parameter. To create an {@code AkkaGrpcClient}, add a parameter with the
 * generated client type to a constructor or {@code @Test} method.
 *
 * <p>Example:
 *
 * <pre>
 * import com.akkaserverless.javasdk.AkkaServerless;
 * import com.akkaserverless.javasdk.testkit.junit.jupiter.AkkaServerlessDescriptor;
 * import com.akkaserverless.javasdk.testkit.junit.jupiter.AkkaServerlessTest;
 *
 * &#64;AkkaServerlessTest
 * class MyAkkaServerlessIntegrationTest {
 *
 *   &#64;AkkaServerlessDescriptor
 *   private static final AkkaServerless MY_AKKA_SERVERLESS = new AkkaServerless(); // with registered services
 *
 *   private final MyServiceClient client; // generated Akka gRPC client
 *
 *   public MyAkkaServerlessIntegrationTest(MyServiceClient client) {
 *     this.client = client;
 *   }
 *
 *   &#64;Test
 *   void test() {
 *     // use client to test service
 *   }
 * }
 * </pre>
 *
 * @see AkkaServerlessDescriptor
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(AkkaServerlessTestkitExtension.class)
@Inherited
public @interface AkkaServerlessTest {}
